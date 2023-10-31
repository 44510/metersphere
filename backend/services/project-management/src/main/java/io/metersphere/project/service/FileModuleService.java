package io.metersphere.project.service;

import io.metersphere.project.domain.FileModule;
import io.metersphere.project.domain.FileModuleExample;
import io.metersphere.project.dto.ModuleCountDTO;
import io.metersphere.project.dto.NodeSortDTO;
import io.metersphere.project.mapper.ExtFileModuleMapper;
import io.metersphere.project.mapper.FileModuleMapper;
import io.metersphere.project.request.filemanagement.FileModuleCreateRequest;
import io.metersphere.project.request.filemanagement.FileModuleUpdateRequest;
import io.metersphere.sdk.constants.ModuleConstants;
import io.metersphere.sdk.exception.MSException;
import io.metersphere.sdk.util.Translator;
import io.metersphere.system.dto.sdk.BaseTreeNode;
import io.metersphere.system.dto.sdk.request.NodeMoveRequest;
import io.metersphere.system.service.CleanupProjectResourceService;
import io.metersphere.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class FileModuleService extends ModuleTreeService implements CleanupProjectResourceService {
    @Resource
    private FileModuleLogService fileModuleLogService;
    @Resource
    private FileModuleMapper fileModuleMapper;
    @Resource
    private ExtFileModuleMapper extFileModuleMapper;
    @Resource
    private SqlSessionFactory sqlSessionFactory;
    @Resource
    private FileManagementService fileManagementService;

    public List<BaseTreeNode> getTree(String projectId) {
        List<BaseTreeNode> fileModuleList = extFileModuleMapper.selectBaseByProjectId(projectId);
        return super.buildTreeAndCountResource(fileModuleList, true, Translator.get("default.module"));
    }

    public List<BaseTreeNode> getTreeOnlyIdsAndResourceCount(String projectId, List<ModuleCountDTO> moduleCountDTOList) {
        //节点内容只有Id和parentId
        List<BaseTreeNode> fileModuleList = extFileModuleMapper.selectIdAndParentIdByProjectId(projectId);
        return super.buildTreeAndCountResource(fileModuleList, moduleCountDTOList, true, Translator.get("default.module"));
    }

    public String add(FileModuleCreateRequest request, String operator) {
        FileModule fileModule = new FileModule();
        fileModule.setId(IDGenerator.nextStr());
        fileModule.setName(request.getName());
        fileModule.setParentId(request.getParentId());
        fileModule.setProjectId(request.getProjectId());
        this.checkDataValidity(fileModule);
        fileModule.setCreateTime(System.currentTimeMillis());
        fileModule.setUpdateTime(fileModule.getCreateTime());
        fileModule.setPos(this.countPos(request.getParentId()));
        fileModule.setCreateUser(operator);
        fileModule.setUpdateUser(operator);
        fileModule.setModuleType(ModuleConstants.NODE_TYPE_DEFAULT);
        fileModuleMapper.insert(fileModule);

        //记录日志
        fileModuleLogService.saveAddLog(fileModule, operator);
        return fileModule.getId();
    }

    private Long countPos(String parentId) {
        Long maxPos = extFileModuleMapper.getMaxPosByParentId(parentId);
        if (maxPos == null) {
            return LIMIT_POS;
        } else {
            return maxPos + LIMIT_POS;
        }
    }

    /**
     * 检查数据的合法性
     */
    private void checkDataValidity(FileModule fileModule) {
        FileModuleExample example = new FileModuleExample();
        if (!StringUtils.equals(fileModule.getParentId(), ModuleConstants.ROOT_NODE_PARENT_ID)) {
            //检查父ID是否存在
            example.createCriteria().andIdEqualTo(fileModule.getParentId());
            if (fileModuleMapper.countByExample(example) == 0) {
                throw new MSException(Translator.get("parent.node.not_blank"));
            }
            example.clear();

            if (StringUtils.isNotBlank(fileModule.getProjectId())) {
                //检查项目ID是否和父节点ID一致
                example.createCriteria().andProjectIdEqualTo(fileModule.getProjectId()).andIdEqualTo(fileModule.getParentId());
                if (fileModuleMapper.countByExample(example) == 0) {
                    throw new MSException(Translator.get("project.cannot.match.parent"));
                }
                example.clear();
            }
        }
        example.createCriteria().andParentIdEqualTo(fileModule.getParentId()).andNameEqualTo(fileModule.getName()).andIdNotEqualTo(fileModule.getId());
        if (fileModuleMapper.countByExample(example) > 0) {
            throw new MSException(Translator.get("node.name.repeat"));
        }
        example.clear();
    }

    public void update(FileModuleUpdateRequest request, String userId) {
        FileModule module = fileModuleMapper.selectByPrimaryKey(request.getId());
        if (module == null) {
            throw new MSException("file_module.not.exist");
        }
        FileModule updateModule = new FileModule();
        updateModule.setId(request.getId());
        updateModule.setName(request.getName());
        updateModule.setParentId(module.getParentId());
        this.checkDataValidity(updateModule);
        updateModule.setUpdateTime(System.currentTimeMillis());
        updateModule.setUpdateUser(userId);
        fileModuleMapper.updateByPrimaryKeySelective(updateModule);
        //记录日志
        fileModuleLogService.saveUpdateLog(updateModule, module.getProjectId(), userId);
    }


    public void deleteModule(String deleteId, String currentUser) {
        FileModule deleteModule = fileModuleMapper.selectByPrimaryKey(deleteId);
        if (deleteModule != null) {
            this.deleteModule(Collections.singletonList(deleteId));
            //记录日志
            fileModuleLogService.saveDeleteLog(deleteModule, currentUser);
        }
    }
    public void deleteModule(List<String> deleteIds) {
        if (CollectionUtils.isEmpty(deleteIds)) {
            return;
        }
        extFileModuleMapper.deleteByIds(deleteIds);
        fileManagementService.deleteByModuleIds(deleteIds);

        List<String> childrenIds = extFileModuleMapper.selectChildrenIdsByParentIds(deleteIds);
        if (CollectionUtils.isNotEmpty(childrenIds)) {
            deleteModule(childrenIds);
        }
    }

    public void moveNode(NodeMoveRequest request, String currentUser) {

        NodeSortDTO nodeSortDTO = super.getNodeSortDTO(request,
                extFileModuleMapper::selectBaseModuleById,
                extFileModuleMapper::selectModuleByParentIdAndPosOperator);

        FileModuleExample example = new FileModuleExample();
        example.createCriteria().andParentIdEqualTo(nodeSortDTO.getParent().getId()).andIdEqualTo(request.getDragNodeId());
        //节点换到了别的节点下,要先更新parent节点再计算sort
        if (fileModuleMapper.countByExample(example) == 0) {
            FileModule fileModule = new FileModule();
            fileModule.setId(request.getDragNodeId());
            fileModule.setParentId(nodeSortDTO.getParent().getId());
            fileModuleMapper.updateByPrimaryKeySelective(fileModule);
        }
        super.sort(nodeSortDTO);
        //记录日志
        fileModuleLogService.saveMoveLog(nodeSortDTO, currentUser);
    }

    /**
     * 查找当前项目下模块每个节点对应的资源统计
     *
     * @param projectId
     * @param moduleCountDTOList
     * @return
     */
    public Map<String, Long> getModuleCountMap(String projectId, List<ModuleCountDTO> moduleCountDTOList) {
        Map<String, Long> returnMap = new HashMap<>();
        //构建模块树，并计算每个节点下的所有数量（包含子节点）
        List<BaseTreeNode> treeNodeList = this.getTreeOnlyIdsAndResourceCount(projectId, moduleCountDTOList);
        //通过广度遍历的方式构建返回值
        List<BaseTreeNode> whileList = new ArrayList<>(treeNodeList);
        while (CollectionUtils.isNotEmpty(whileList)) {
            List<BaseTreeNode> childList = new ArrayList<>();
            for (BaseTreeNode treeNode : whileList) {
                returnMap.put(treeNode.getId(), treeNode.getCount());
                childList.addAll(treeNode.getChildren());
            }
            whileList = childList;
        }
        return returnMap;
    }


    @Override
    public void updatePos(String id, long pos) {
        FileModule updateModule = new FileModule();
        updateModule.setPos(pos);
        updateModule.setId(id);
        fileModuleMapper.updateByPrimaryKeySelective(updateModule);
    }

    @Override
    public void refreshPos(String parentId) {
        List<String> childrenIdSortByPos = extFileModuleMapper.selectChildrenIdsSortByPos(parentId);
        SqlSession sqlSession = sqlSessionFactory.openSession(ExecutorType.BATCH);
        FileModuleMapper batchUpdateMapper = sqlSession.getMapper(FileModuleMapper.class);
        for (int i = 0; i < childrenIdSortByPos.size(); i++) {
            String nodeId = childrenIdSortByPos.get(i);
            FileModule updateModule = new FileModule();
            updateModule.setId(nodeId);
            updateModule.setPos((i + 1) * LIMIT_POS);
            batchUpdateMapper.updateByPrimaryKeySelective(updateModule);
        }
        sqlSession.flushStatements();
        SqlSessionUtils.closeSqlSession(sqlSession, sqlSessionFactory);
    }

    @Override
    public void deleteResources(String projectId) {
        List<String> fileModuleIdList = extFileModuleMapper.selectIdsByProjectId(projectId);
        if (CollectionUtils.isNotEmpty(fileModuleIdList)) {
            this.deleteModule(fileModuleIdList);
        }
    }

    @Override
    public void cleanReportResources(String projectId) {
        // nothing to do
    }

    public Map<String, String> getModuleNameMapByIds(List<String> moduleIds) {
        if (CollectionUtils.isEmpty(moduleIds)) {
            return new HashMap<>();
        } else {
            FileModuleExample example = new FileModuleExample();
            example.createCriteria().andIdIn(moduleIds);
            List<FileModule> moduleList = fileModuleMapper.selectByExample(example);
            return moduleList.stream().collect(Collectors.toMap(FileModule::getId, FileModule::getName));
        }
    }

    public String getModuleName(String moduleId) {
        return extFileModuleMapper.selectNameById(moduleId);
    }
}
