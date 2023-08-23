package io.metersphere.system.controller;


import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.metersphere.project.domain.Project;
import io.metersphere.sdk.constants.PermissionConstants;
import io.metersphere.sdk.constants.UserSource;
import io.metersphere.sdk.dto.*;
import io.metersphere.sdk.log.annotation.Log;
import io.metersphere.sdk.log.constants.OperationLogType;
import io.metersphere.sdk.util.PageUtils;
import io.metersphere.sdk.util.Pager;
import io.metersphere.sdk.util.SessionUtils;
import io.metersphere.system.domain.Organization;
import io.metersphere.system.dto.UserBatchCreateDTO;
import io.metersphere.system.request.OrganizationMemberBatchRequest;
import io.metersphere.system.request.ProjectAddMemberBatchRequest;
import io.metersphere.system.request.user.UserChangeEnableRequest;
import io.metersphere.system.request.user.UserEditRequest;
import io.metersphere.system.request.user.UserRoleBatchRelationRequest;
import io.metersphere.system.response.user.UserImportResponse;
import io.metersphere.system.response.user.UserSelectOption;
import io.metersphere.system.response.user.UserTableResponse;
import io.metersphere.system.response.user.UserTreeSelectOption;
import io.metersphere.system.service.*;
import io.metersphere.system.utils.TreeNodeParseUtils;
import io.metersphere.validation.groups.Created;
import io.metersphere.validation.groups.Updated;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/system/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private GlobalUserRoleService globalUserRoleService;
    @Resource
    private GlobalUserRoleRelationService globalUserRoleRelationService;
    @Resource
    private OrganizationService organizationService;
    @Resource
    private SystemProjectService systemProjectService;
    @Resource
    private UserLogService userLogService;

    @GetMapping("/get/{keyword}")
    @Operation(summary = "通过email或id查找用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ)
    public UserDTO getUser(@PathVariable String keyword) {
        return userService.getUserDTOByKeyword(keyword);
    }

    @PostMapping("/add")
    @Operation(summary = "添加用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ_ADD)
    public UserBatchCreateDTO addUser(@Validated({Created.class}) @RequestBody UserBatchCreateDTO userCreateDTO) {
        return userService.addUser(userCreateDTO, UserSource.LOCAL.name(), SessionUtils.getUserId());
    }

    @PostMapping("/update")
    @Operation(summary = "修改用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.updateLog(#request)", msClass = UserLogService.class)
    public UserEditRequest updateUser(@Validated({Updated.class}) @RequestBody UserEditRequest request) {
        return userService.updateUser(request, SessionUtils.getUserId());
    }

    @PostMapping("/page")
    @Operation(summary = "分页查找用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ)
    public Pager<List<UserTableResponse>> list(@Validated @RequestBody BasePageRequest request) {
        Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
                StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "create_time desc");
        return PageUtils.setPageInfo(page, userService.list(request));
    }

    @PostMapping("/update/enable")
    @Operation(summary = "启用/禁用用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.batchUpdateLog(#request)", msClass = UserLogService.class)
    public TableBatchProcessResponse updateUserEnable(@Validated @RequestBody UserChangeEnableRequest request) {
        return userService.updateUserEnable(request, SessionUtils.getSessionId());
    }

    @PostMapping(value = "/import", consumes = {"multipart/form-data"})
    @Operation(summary = "导入用户")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ_IMPORT)
    public UserImportResponse importUser(@RequestPart(value = "file", required = false) MultipartFile excelFile) {
        return userService.importByExcel(excelFile, UserSource.LOCAL.name(), SessionUtils.getSessionId());
    }

    @PostMapping("/delete")
    @Operation(summary = "删除用户")
    @Log(type = OperationLogType.DELETE, expression = "#msClass.deleteLog(#request)", msClass = UserLogService.class)
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ_DELETE)
    public TableBatchProcessResponse deleteUser(@Validated @RequestBody TableBatchProcessDTO request) {
        return userService.deleteUser(request, SessionUtils.getUserId());
    }

    @PostMapping("/reset/password")
    @Operation(summary = "重置用户密码")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ_UPDATE)
    @Log(type = OperationLogType.UPDATE, expression = "#msClass.resetPasswordLog(#request)", msClass = UserLogService.class)
    public TableBatchProcessResponse resetPassword(@Validated @RequestBody TableBatchProcessDTO request) {
        return userService.resetPassword(request, SessionUtils.getUserId());
    }

    @GetMapping("/get/global/system/role")
    @Operation(summary = "查找系统级用户权限")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_ROLE_READ)
    public List<UserSelectOption> getGlobalSystemRole() {
        return globalUserRoleService.getGlobalSystemRoleList();
    }

    @GetMapping("/get/organization")
    @Operation(summary = "用户批量操作-查找组织")
    @RequiresPermissions(value = {PermissionConstants.SYSTEM_USER_ROLE_READ, PermissionConstants.SYSTEM_ORGANIZATION_PROJECT_READ}, logical = Logical.AND)
    public List<OptionDTO> getOrganization() {
        return organizationService.listAll();
    }

    @GetMapping("/get/project")
    @Operation(summary = "用户批量操作-查找项目")
    @RequiresPermissions(value = {PermissionConstants.SYSTEM_USER_ROLE_READ, PermissionConstants.SYSTEM_ORGANIZATION_PROJECT_READ}, logical = Logical.AND)
    public List<UserTreeSelectOption> getProject() {
        Map<Organization, List<Project>> orgProjectMap = organizationService.getOrgProjectMap();
        return TreeNodeParseUtils.parseOrgProjectMap(orgProjectMap);
    }

    @PostMapping("/add/batch/user-role")
    @Operation(summary = "批量添加用户到多个用户组中")
    @RequiresPermissions(PermissionConstants.SYSTEM_USER_READ_UPDATE)
    @Log(type = OperationLogType.ADD, expression = "#msClass.batchAddLog(#request)", msClass = GlobalUserRoleRelationLogService.class)
    public TableBatchProcessResponse batchAddUserGroupRole(@Validated({Created.class}) @RequestBody UserRoleBatchRelationRequest request) {
        return globalUserRoleRelationService.batchAdd(request, SessionUtils.getUserId());
    }

    @PostMapping("/add-project-member")
    @Operation(summary = "批量添加用户到项目")
    @RequiresPermissions(value = {PermissionConstants.SYSTEM_USER_READ_UPDATE, PermissionConstants.SYSTEM_ORGANIZATION_PROJECT_MEMBER_ADD}, logical = Logical.AND)
    public TableBatchProcessResponse addProjectMember(@Validated @RequestBody UserRoleBatchRelationRequest userRoleBatchRelationRequest) {
        ProjectAddMemberBatchRequest request = new ProjectAddMemberBatchRequest();
        request.setProjectIds(userRoleBatchRelationRequest.getRoleIds());
        request.setUserIds(userRoleBatchRelationRequest.getSelectIds());
        systemProjectService.addProjectMember(request, SessionUtils.getUserId());
        userLogService.batchAddProjectLog(userRoleBatchRelationRequest, SessionUtils.getUserId());
        return new TableBatchProcessResponse(userRoleBatchRelationRequest.getSelectIds().size(), userRoleBatchRelationRequest.getSelectIds().size());
    }

    @PostMapping("/add-org-member")
    @Operation(summary = "批量添加用户到组织")
    @RequiresPermissions(value = {PermissionConstants.SYSTEM_USER_READ_UPDATE, PermissionConstants.SYSTEM_ORGANIZATION_PROJECT_MEMBER_ADD}, logical = Logical.AND)
    public TableBatchProcessResponse addMember(@Validated @RequestBody UserRoleBatchRelationRequest userRoleBatchRelationRequest) {
        //获取本次处理的用户
        userRoleBatchRelationRequest.setSelectIds(userService.getBatchUserIds(userRoleBatchRelationRequest));
        OrganizationMemberBatchRequest request = new OrganizationMemberBatchRequest();
        request.setOrganizationIds(userRoleBatchRelationRequest.getRoleIds());
        request.setUserIds(userRoleBatchRelationRequest.getSelectIds());
        organizationService.addMemberBySystem(request, SessionUtils.getUserId());
        userLogService.batchAddOrgLog(userRoleBatchRelationRequest, SessionUtils.getUserId());
        return new TableBatchProcessResponse(userRoleBatchRelationRequest.getSelectIds().size(), userRoleBatchRelationRequest.getSelectIds().size());
    }
}
