import el from "metersphere-frontend/src/i18n/lang/ele-zh-CN"; // 加载element的内容
import fu from "fit2cloud-ui/src/locale/lang/zh-CN"; // 加载fit2cloud的内容
import mf from "metersphere-frontend/src/i18n/lang/zh-CN";

const message = {
  home: {
    table: {
      index: "序号",
      task_type: "任务类型",
      run_rule: "运行规则",
      task_status: "任务状态",
      next_execution_time: "下次执行时间",
      create_user: "创建人",
      update_time: "更新时间",
    },
    case: {
      index: "排名",
      case_name: "用例名称",
      case_type: "用例类型",
      test_plan: "所属测试计划",
      failure_times: "失败次数",
    },
    rate: {
      case_review: "评审率",
      case_review_pass: "评审通过率",
      cover: "覆盖率",
      legacy: "遗留率",
      legacy_issue: "遗留缺陷占比",
    },
    dashboard: {
      public: {
        this_week: "本周",
        load_error: "加载失败",
        no_data: "暂无数据",
        no_search_data: "没有找到相关内容",
      },
      case_finished_review_pass_tip: "已评审通过的案例/所有完成评审的案例*100%",
    },
    case_review_dashboard: {
      case_count: "用例数量",
      not_review: "未评审",
      finished_review: "已评审",
      not_pass: "未通过",
      pass: "已通过",
    },
    relevance_dashboard: {
      api_case: "接口用例",
      scenario_case: "场景用例",
      performance_case: "性能用例",
      relevance_case_count: "关联用例数量",
      not_cover: "未覆盖",
      cover: "已覆盖",
    },
    bug_dashboard: {
      un_closed_bug_count: "遗留缺陷数",
      un_closed_range: "遗留率",
      un_closed_range_tips: "未关闭缺陷/所有关联的缺陷*100%",
      un_closed_bug_case_range: "遗留缺陷占比",
      un_closed_bug_case_range_tips: "未关闭缺陷/所有关联的用例*100%",
      un_closed_count: "遗留缺陷数",
      total_count: "缺陷总数",
      case_count: "用例总数",
    },
  },
  plan: {
    batch_delete_tip: "批量删除测试计划，是否继续?",
    relevance_case_success: "已添加至测试计划"
  },
  review: {
    result_distribution: "结果分布",
    review_pass_rule: "评审通过标准",
    review_pass_rule_all: "全部通过",
    review_pass_rule_single: "单人通过",
    update_review_reviewer_tip: "注：修改评审人，会覆盖已关联用例的评审人，并且更新用例的评审状态，请谨慎操作！",
    review_rule_tip: "全部通过：所有评审人都通过才通过<\/br>单人通过：任意评审人通过则通过",
    update_review_rule_tip: "注：修改通过标准会影响已评审过的用例，请谨慎操作！",
    review_history: "评审历史",
    no_review_history: "暂无评审历史",
    added_comment: "添加了评论",
    un_pass_review_confirm: "确定不通过此评审吗",
    please_input_review_comment: "请输入评审意见",
    pass_review_confirm: "确定通过此评审吗",
    comment_require: "(必填)",
    comment_not_require: "(选填)",
    search_by_id_or_name_or_tag: "通过ID/名称/标签搜索"
  },
  case: {
    all_case_content: "全部用例",
    use_case_detail: "用例详情",
    associate_test_cases: "关联测试用例",
    dependencies: "依赖关系",
    comment: "评论",
    change_record: "变更记录",
    case_name: "用例名称",
    please_enter_the_case_name: "请输入用例名称",
    preconditions: "前置条件",
    please_enter_preconditions: "请输入前置条件",
    attachment: "附件",
    none: "暂无",
    commented: "发布了评论",
    add_attachment: "添加附件",
    file_size_limit: "支持任意类型文件，文件大小不超过 500MB",
    file_size_out_of_bounds: "文件大小不超过 500MB",
    upload_at: "上传于",
    relate_at: "关联于",
    add_steps: "添加步骤",
    insert_steps: "插入步骤",
    copy_this_step: "复制该步骤",
    more: "更多",
    follow: "关注",
    followed: "已关注",
    previous_public_case: "上一条",
    next_public_case: "下一条",
    add_to_public_case: "添加到公共用例库",
    added_to_public_case: "已添加到公共用例库",
    make_comment: "发表评论",
    please_enter_comment: "请输入评论",
    associated_defect: "关联缺陷",
    create_defect: "创建缺陷",
    associate_existing_defects: "关联现有缺陷",
    search_by_id: "通过ID或名称搜索",
    relieve: "解除依赖",
    content_before_change: "变更前内容",
    content_after_change: "变更后内容",
    empty_tip: "空值",
    all_case: "全部用例",
    all_scenes: "全部场景",
    all_api: "全部接口",
    associated_files:"关联文件",
    empty_file: "暂无文件",
    upload_file: "上传文件",
    selected: "已选择",
    strip: "条",
    clear: "清空",
    please_enter_a_text_description: "请输入文本描述",
    please_enter_expected_results: "请输入预期结果",
    please_enter_comments: "请输入备注",
    disassociate: "取消关联",
    saveAndCreate: "保存并继续添加",
    last_version: "最新版本",
    set_new: "置新",
    version_comparison: "版本对比",
    compare: "对比",
    project: "项目",
    create_version: "新建版本",
    choose_copy_info: "选择复制信息",
    current_display_history_version: "当前展示历史版本",
    compare_with_the_latest_version: "与最新版本比较",
    view_the_latest_version: "查看最新版本",
    version_id_cannot_be_empty: "版本号不能为空",
    enter_comments_and_click_send: "输入评论,点击发送",
    cancel_relate_case_tips_title: "确定取消关联关系吗?",
    cancel_relate_case_tips_content: "取消关联会影响测试计划相关统计, 确定取消吗?",
    back_tips: "你填写的信息未保存, 确定退出吗?",
    dependency_remove_confirm: "确定解除依赖吗?",
    minder_paste_tip: "粘贴的节点中有未加载用例的模块，目前不支持复制未加载的用例！",
    minder_move_confirm_tip: "当前存在字段排序，无法设置用例顺序，请切换至用例列表，取消字段排序!",
    minder_module_move_confirm_tip: "模块不支持设置顺序!",
    public: {
      remove: '是否移除用例',
      batch_remove_confirm: "确定移除{0}项用例?",
    },
    enter_issues_content: "请输入缺陷内容...",
  },
  attachment: {
    preview: "预览",
    download: "下载",
    dump: "转存",
    unRelate: "取消关联",
    delete: "删除",
    delete_confirm_tips: "是否删除此项?",
    upload_success: "上传成功",
    upload_error: "上传失败",
    not_exits: "该文件不存在",
    waiting_upload: "等待上传",
    waiting_relate: "等待关联",
  }
};

export default {
  ...el,
  ...fu,
  ...mf,
  ...message,
};
