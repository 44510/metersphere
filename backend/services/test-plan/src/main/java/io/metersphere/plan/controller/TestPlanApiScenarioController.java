package io.metersphere.plan.controller;

import io.metersphere.plan.dto.LogInsertModule;
import io.metersphere.plan.dto.request.ResourceSortRequest;
import io.metersphere.plan.dto.request.TestPlanAssociationRequest;
import io.metersphere.plan.dto.response.TestPlanAssociationResponse;
import io.metersphere.plan.dto.response.TestPlanResourceSortResponse;
import io.metersphere.plan.service.TestPlanApiScenarioService;
import io.metersphere.sdk.constants.HttpMethodConstants;
import io.metersphere.sdk.constants.PermissionConstants;
import io.metersphere.system.security.CheckOwner;
import io.metersphere.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "测试计划场景用例")
@RestController
@RequestMapping("/test-plan/api/scenario")
public class TestPlanApiScenarioController {

    @Resource
    private TestPlanApiScenarioService testPlanApiScenarioService;

    @PostMapping(value = "/association")
    @Operation(summary = "测试计划场景用例-关联接口用例")
    @RequiresPermissions(PermissionConstants.TEST_PLAN_READ_UPDATE)
    @CheckOwner(resourceId = "#request.getTestPlanId()", resourceType = "test_plan")
    public TestPlanAssociationResponse association(@Validated @RequestBody TestPlanAssociationRequest request) {
        return testPlanApiScenarioService.association(request, new LogInsertModule(SessionUtils.getUserId(), "/test-plan/api/scenario/association", HttpMethodConstants.POST.name()));
    }

    @PostMapping(value = "/sort")
    @Operation(summary = "测试计划场景用例-关联功能用例")
    @RequiresPermissions(PermissionConstants.TEST_PLAN_READ_UPDATE)
    @CheckOwner(resourceId = "#request.getTestPlanId()", resourceType = "test_plan")
    public TestPlanResourceSortResponse sortNode(@Validated @RequestBody ResourceSortRequest request) {
        return testPlanApiScenarioService.sortNode(request, new LogInsertModule(SessionUtils.getUserId(), "/test-plan/api/scenario/sort", HttpMethodConstants.POST.name()));
    }

}
