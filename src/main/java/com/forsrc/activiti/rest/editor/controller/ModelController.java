package com.forsrc.activiti.rest.editor.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.Model;
import org.activiti.explorer.util.XmlUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(description = "Model", tags = { "modeler" })
@RestController
@RequestMapping("models")
public class ModelController {

    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private RepositoryService repositoryService;

    @ApiOperation(value = "create a model")
    @PostMapping
    public Map<String, Object> createModel() throws UnsupportedEncodingException {
        RepositoryService repositoryService = processEngine.getRepositoryService();

        Model model = repositoryService.newModel();

        String name = "process-name";
        String description = "description";
        int revision = 1;
        String key = "process";

        ObjectNode modelNode = objectMapper.createObjectNode();
        modelNode.put(ModelDataJsonConstants.MODEL_NAME, name);
        modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
        modelNode.put(ModelDataJsonConstants.MODEL_REVISION, revision);

        model.setName(name);
        model.setKey(key);
        model.setMetaInfo(modelNode.toString());

        repositoryService.saveModel(model);
        String id = model.getId();

        ObjectNode editorNode = objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.put("stencilset", stencilSetNode);
        repositoryService.addModelEditorSource(id, editorNode.toString().getBytes("utf-8"));
        // return new ModelAndView("redirect:/modeler.html?modelId=" + id);
        return success();
    }

    @ApiOperation(value = "list")
    @GetMapping
    public List<Model> modelList() {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        return repositoryService.createModelQuery().orderByCreateTime().desc().list();
    }

    @ApiOperation(value = "delete")
    @DeleteMapping("{id}")
    public Object deleteModel(@PathVariable("id") String id) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        repositoryService.deleteModel(id);
        return success();
    }

    @ApiOperation(value = "deployment")
    @PostMapping("{id}/deployment")
    public Object deploy(@PathVariable("id") String id) throws Exception {

        RepositoryService repositoryService = processEngine.getRepositoryService();
        Model modelData = repositoryService.getModel(id);
        byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());

        if (bytes == null) {
            return failed("model editor source is null");
        }

        JsonNode modelNode = new ObjectMapper().readTree(bytes);

        BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        if (model.getProcesses().size() == 0) {
            return failed("processes is empty");
        }
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

        String processName = modelData.getName() + ".bpmn20.xml";
        DeploymentBuilder deploymentBuilder = repositoryService.createDeployment().name(modelData.getName())
                .addString(processName, new String(bpmnBytes, "UTF-8"));

        Deployment deployment = deploymentBuilder.deploy();
        modelData.setDeploymentId(deployment.getId());
        repositoryService.saveModel(modelData);

        return success();
    }

    @ApiOperation(value = "upload")
    @RequestMapping(value = "/uploadFile", method = RequestMethod.POST)
    public Object deployUploadedFile(@RequestParam("uploadfile") MultipartFile uploadfile) {

        String fileName = uploadfile.getOriginalFilename();
        if (!fileName.endsWith(".bpmn20.xml") && !fileName.endsWith(".bpmn")) {
            return failed("the file is not .bpmn20.xml/*.bpmn");
        }
        try {
            BpmnModel bpmnModel = null;
            XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
            try (InputStreamReader in = new InputStreamReader(new ByteArrayInputStream(uploadfile.getBytes()),
                    "UTF-8")) {
                XMLStreamReader xtr = xif.createXMLStreamReader(in);
                bpmnModel = new BpmnXMLConverter().convertToBpmnModel(xtr);
            }

            if (bpmnModel.getMainProcess() == null) {
                // notificationManager.showErrorNotification(Messages.MODEL_IMPORT_FAILED,
                // i18nManager.getMessage(Messages.MODEL_IMPORT_INVALID_BPMN_EXPLANATION));
                return failed("main process is null");
            }

            if (bpmnModel.getMainProcess().getId() == null) {
                // notificationManager.showErrorNotification(Messages.MODEL_IMPORT_FAILED,
                // i18nManager.getMessage(Messages.MODEL_IMPORT_INVALID_BPMN_EXPLANATION));
                return failed("main process id is null");
            }

            if (bpmnModel.getLocationMap().isEmpty()) {
                // notificationManager.showErrorNotification(Messages.MODEL_IMPORT_INVALID_BPMNDI,
                // i18nManager.getMessage(Messages.MODEL_IMPORT_INVALID_BPMNDI_EXPLANATION));
                return failed("location map is empty");
            }

            String processName = null;
            if (StringUtils.isNotEmpty(bpmnModel.getMainProcess().getName())) {
                processName = bpmnModel.getMainProcess().getName();
            } else {
                processName = bpmnModel.getMainProcess().getId();
            }
            Model modelData;
            modelData = repositoryService.newModel();
            ObjectNode modelObjectNode = new ObjectMapper().createObjectNode();
            modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, processName);
            modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
            modelData.setMetaInfo(modelObjectNode.toString());
            modelData.setName(processName);

            repositoryService.saveModel(modelData);

            BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
            ObjectNode editorNode = jsonConverter.convertToJson(bpmnModel);

            repositoryService.addModelEditorSource(modelData.getId(), editorNode.toString().getBytes("utf-8"));

        } catch (Exception e) {
            String errorMsg = e.getMessage().replace(System.getProperty("line.separator"), "<br/>");
            // notificationManager.showErrorNotification(Messages.MODEL_IMPORT_FAILED,
            // errorMsg);
            return failed(errorMsg);
        }
        return success();
    }

    private Map<String, Object> success() {
        Map<String, Object> map = new HashMap<>();
        map.put("status", true);
        map.put("reason", "success");
        return map;
    }

    private Map<String, Object> failed(String reason) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", false);
        map.put("reason", "failed：" + reason);
        return map;
    }

}
