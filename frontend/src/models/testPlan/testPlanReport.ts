export interface countDetail {
  success: number;
  error: number;
  fakeError: number;
  block: number;
  pending: number;
}
export interface PlanReportDetail {
  id: string;
  name: string;
  startTime: number;
  executeTime: number; // 报告执行开始时间
  endTime: number;
  summary: string;
  passThreshold: number; // 通过阈值
  passRate: number; // 通过率
  executeRate: number; // 执行完成率
  bugCount: number;
  caseTotal: number;
  executeCount: countDetail;
  functionalCount: countDetail;
  // TOTO 这个版本不展示场景和接口
  // apiCaseCount: countDetail; // 接口场景用例分析-用例数
  // apiScenarioCount: countDetail; // 接口场景用例分析-用例数
}

export interface StatusListType {
  label: string;
  value: keyof countDetail;
  color: string;
  class: string;
  rateKey: string;
  key: string;
}
