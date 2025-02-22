<template>
  <div :class="['p-[16px_22px]', props.class]">
    <div :class="['mb-[16px]', 'flex', 'items-center', props.isApi ? 'justify-between' : 'justify-end']">
      <a-button
        v-show="props.isApi"
        v-permission="['PROJECT_API_DEFINITION_MOCK:READ+ADD']"
        type="primary"
        @click="createMock"
      >
        {{ t('mockManagement.createMock') }}
      </a-button>
      <div class="flex gap-[8px]">
        <a-input-search
          v-model:model-value="keyword"
          :placeholder="t('apiTestManagement.searchPlaceholder')"
          allow-clear
          class="mr-[8px] w-[240px]"
          @search="loadMockList"
          @press-enter="loadMockList"
          @clear="loadMockList"
        />
        <a-button type="outline" class="arco-btn-outline--secondary !p-[8px]" @click="loadMockList">
          <template #icon>
            <icon-refresh class="text-[var(--color-text-4)]" />
          </template>
        </a-button>
      </div>
    </div>
    <ms-base-table
      v-bind="propsRes"
      :action-config="batchActions"
      :first-column-width="44"
      no-disable
      filter-icon-align-left
      v-on="propsEvent"
      @selected-change="handleTableSelect"
      @batch-action="handleTableBatch"
      @module-change="loadMockList()"
    >
      <template #expectNum="{ record }">
        <MsButton type="text" @click="handleOpenDetail(record)">
          {{ record.expectNum }}
        </MsButton>
      </template>
      <template #enable="{ record }">
        <a-switch
          v-model="record.enable"
          type="line"
          :before-change="() => handleBeforeEnableChange(record)"
          :disabled="!hasAnyPermission(['PROJECT_API_DEFINITION_MOCK:READ+UPDATE'])"
        ></a-switch>
      </template>
      <template #apiMethod="{ record }">
        <apiMethodName :method="record.apiMethod" is-tag />
      </template>
      <template #action="{ record }">
        <MsButton
          v-permission="['PROJECT_API_DEFINITION_MOCK:READ+UPDATE']"
          type="text"
          class="!mr-0"
          @click="editMock(record)"
        >
          {{ t('common.edit') }}
        </MsButton>
        <a-divider direction="vertical" :margin="8"></a-divider>
        <MsButton
          v-permission="['PROJECT_API_DEFINITION:READ+EXECUTE']"
          type="text"
          class="!mr-0"
          @click="debugMock(record)"
        >
          {{ t('apiTestManagement.debug') }}
        </MsButton>
        <a-divider direction="vertical" :margin="8"></a-divider>
        <MsButton
          v-permission="['PROJECT_API_DEFINITION_MOCK:READ+ADD']"
          type="text"
          class="!mr-0"
          @click="handleCopyMock(record)"
        >
          {{ t('common.copy') }}
        </MsButton>
        <a-divider direction="vertical" :margin="8"></a-divider>
        <MsTableMoreAction :list="tableMoreActionList" @select="handleTableMoreActionSelect($event, record)" />
      </template>
      <template v-if="hasAnyPermission(['PROJECT_API_DEFINITION_MOCK:READ+ADD']) && props.isApi" #empty>
        <div class="flex w-full items-center justify-center p-[8px] text-[var(--color-text-4)]">
          {{ t('apiTestManagement.tableNoDataAndPlease') }}
          <MsButton class="ml-[8px]" @click="createMock">
            {{ t('mockManagement.createMock') }}
          </MsButton>
        </div>
      </template>
    </ms-base-table>
  </div>
  <a-modal v-model:visible="showBatchModal" title-align="start" class="ms-modal-upload ms-modal-medium" :width="480">
    <template #title>
      {{ t('common.edit') }}
      <div class="text-[var(--color-text-4)]">
        {{
          t('apiTestManagement.batchModalSubTitle', {
            count: batchParams.currentSelectCount || tableSelected.length,
          })
        }}
      </div>
    </template>
    <a-form ref="batchFormRef" class="rounded-[4px]" :model="batchForm" layout="vertical">
      <a-form-item
        field="attr"
        :label="t('apiTestManagement.chooseAttr')"
        :rules="[{ required: true, message: t('apiTestManagement.attrRequired') }]"
        asterisk-position="end"
      >
        <a-select v-model="batchForm.attr" :placeholder="t('common.pleaseSelect')">
          <a-option v-for="item of attrOptions" :key="item.value" :value="item.value">
            {{ t(item.name) }}
          </a-option>
        </a-select>
      </a-form-item>
      <a-form-item
        v-if="batchForm.attr === 'Tags'"
        field="values"
        :label="t('apiTestManagement.batchUpdate')"
        :validate-trigger="['blur', 'input']"
        :rules="[{ required: true, message: t('apiTestManagement.valueRequired') }]"
        asterisk-position="end"
        class="mb-0"
        required
      >
        <MsTagsInput
          v-model:model-value="batchForm.values"
          placeholder="common.tagsInputPlaceholder"
          allow-clear
          unique-value
          retain-input-value
        />
      </a-form-item>
      <a-form-item
        v-else
        field="value"
        :label="t('apiTestManagement.batchUpdate')"
        :rules="[{ required: true, message: t('apiTestManagement.valueRequired') }]"
        asterisk-position="end"
        class="mb-0"
      >
        <a-radio-group v-model:model-value="batchForm.value">
          <a-radio :value="true">
            {{ t('common.enable') }}
          </a-radio>
          <a-radio :value="false">
            {{ t('common.disable') }}
          </a-radio>
        </a-radio-group>
      </a-form-item>
    </a-form>
    <template #footer>
      <div class="flex" :class="[batchForm.attr === 'Tags' ? 'justify-between' : 'justify-end']">
        <div
          v-if="batchForm.attr === 'Tags'"
          class="flex flex-row items-center justify-center"
          style="padding-top: 10px"
        >
          <a-switch v-model="batchForm.append" class="mr-1" size="small" type="line" />
          <span class="flex items-center">
            <span class="mr-1">{{ t('caseManagement.featureCase.appendTag') }}</span>
            <span class="mt-[2px]">
              <a-tooltip>
                <IconQuestionCircle class="h-[16px] w-[16px] text-[rgb(var(--primary-5))]" />
                <template #content>
                  <div>{{ t('caseManagement.featureCase.enableTags') }}</div>
                  <div>{{ t('caseManagement.featureCase.closeTags') }}</div>
                </template>
              </a-tooltip>
            </span>
          </span>
        </div>
        <div class="flex justify-end">
          <a-button type="secondary" :disabled="batchUpdateLoading" @click="cancelBatch">
            {{ t('common.cancel') }}
          </a-button>
          <a-button class="ml-3" type="primary" :loading="batchUpdateLoading" @click="batchUpdate">
            {{ t('common.update') }}
          </a-button>
        </div>
      </div>
    </template>
  </a-modal>
  <mockDetailDrawer
    v-model:visible="mockDetailDrawerVisible"
    :definition-detail="mockBelongDefinitionDetail"
    :detail-id="activeMockRecord?.id"
    :is-copy="isCopy"
    :is-edit-mode="isEdit"
    @add-done="loadMockList"
    @delete="() => removeMock(activeMockRecord)"
  />
</template>

<script setup lang="ts">
  import { useClipboard } from '@vueuse/core';
  import { FormInstance, Message } from '@arco-design/web-vue';

  import MsButton from '@/components/pure/ms-button/index.vue';
  import MsBaseTable from '@/components/pure/ms-table/base-table.vue';
  import type { BatchActionParams, BatchActionQueryParams, MsTableColumn } from '@/components/pure/ms-table/type';
  import useTable from '@/components/pure/ms-table/useTable';
  import MsTableMoreAction from '@/components/pure/ms-table-more-action/index.vue';
  import { ActionsItem } from '@/components/pure/ms-table-more-action/types';
  import MsTagsInput from '@/components/pure/ms-tags-input/index.vue';
  import mockDetailDrawer from './mockDetailDrawer.vue';
  import apiMethodName from '@/views/api-test/components/apiMethodName.vue';
  import { RequestParam } from '@/views/api-test/components/requestComposition/index.vue';

  import {
    batchDeleteMock,
    batchEditMock,
    deleteMock,
    getDefinitionDetail,
    getDefinitionMockPage,
    getMockDetail,
    getMockUrl,
    updateMockStatusPage,
  } from '@/api/modules/api-test/management';
  import { useI18n } from '@/hooks/useI18n';
  import useModal from '@/hooks/useModal';
  import useTableStore from '@/hooks/useTableStore';
  import useAppStore from '@/store/modules/app';
  import { operationWidth } from '@/utils';
  import { hasAnyPermission } from '@/utils/permission';

  import { ApiDefinitionMockDetail } from '@/models/apiTest/management';
  import { MockDetail } from '@/models/apiTest/mock';
  import { RequestComposition } from '@/enums/apiEnum';
  import { TableKeyEnum } from '@/enums/tableEnum';

  const props = defineProps<{
    isApi?: boolean; // 接口定义详情的case tab下
    class?: string;
    activeModule: string;
    offspringIds: string[];
    definitionDetail: RequestParam;
    readOnly?: boolean; // 是否是只读模式
    protocol: string; // 查看的协议类型
    heightUsed?: number;
  }>();
  const emit = defineEmits<{
    (e: 'init', params: any): void;
    (e: 'change'): void;
    (e: 'debug', mock: MockDetail): void;
  }>();

  const appStore = useAppStore();
  const tableStore = useTableStore();
  const { t } = useI18n();
  const { openModal } = useModal();

  const keyword = ref('');

  let columns: MsTableColumn = [
    {
      title: 'ID',
      dataIndex: 'expectNum',
      slotName: 'expectNum',
      sortIndex: 1,
      sortable: {
        sortDirections: ['ascend', 'descend'],
        sorter: true,
      },
      fixed: 'left',
      width: 120,
    },
    {
      title: 'mockManagement.name',
      dataIndex: 'name',
      showTooltip: true,
      sortable: {
        sortDirections: ['ascend', 'descend'],
        sorter: true,
      },
      width: 200,
    },
    {
      title: 'apiTestManagement.apiType',
      dataIndex: 'apiMethod',
      slotName: 'apiMethod',
      width: 200,
      showDrag: true,
    },
    {
      title: 'mockManagement.apiPath',
      dataIndex: 'apiPath',
      slotName: 'apiPath',
      showTooltip: true,
      width: 200,
      showDrag: true,
    },
    {
      title: 'common.tag',
      dataIndex: 'tags',
      isTag: true,
      isStringTag: true,
      showDrag: true,
    },
    {
      title: 'common.status',
      dataIndex: 'enable',
      slotName: 'enable',
      width: 100,
      showDrag: true,
    },
    {
      title: 'mockManagement.operationUser',
      slotName: 'createUserName',
      dataIndex: 'createUserName',
      width: 200,
      showDrag: true,
    },
    {
      title: 'mockManagement.updateTime',
      dataIndex: 'updateTime',
      sortable: {
        sortDirections: ['ascend', 'descend'],
        sorter: true,
      },
      showDrag: true,
      width: 180,
    },
    {
      title: 'common.operation',
      slotName: 'action',
      dataIndex: 'operation',
      fixed: 'right',
      width: operationWidth(230, 200),
    },
  ];
  const { propsRes, propsEvent, loadList, setLoadListParams, resetSelector } = useTable(getDefinitionMockPage, {
    columns: props.readOnly ? columns : [],
    scroll: { x: '100%' },
    tableKey: props.readOnly ? undefined : TableKeyEnum.API_TEST_MANAGEMENT_MOCK,
    showSetting: !props.readOnly,
    selectable: true,
    heightUsed: (props.heightUsed || 0) + 282,
    showSelectAll: !props.readOnly,
    draggable: props.readOnly ? undefined : { type: 'handle', width: 32 },
    showSubdirectory: true,
    paginationSize: 'mini',
  });
  const batchActions = {
    baseAction: [
      {
        label: 'mockManagement.batchEdit',
        eventTag: 'edit',
        permission: ['PROJECT_API_DEFINITION_MOCK:READ+UPDATE'],
      },
      {
        label: 'mockManagement.batchDelete',
        eventTag: 'delete',
        danger: true,
        permission: ['PROJECT_API_DEFINITION_MOCK:READ+DELETE'],
      },
    ],
  };
  const tableMoreActionList = [
    {
      eventTag: 'copyMock',
      label: t('mockManagement.copyMock'),
      danger: false,
      permission: ['PROJECT_API_DEFINITION_MOCK:READ'],
    },
    {
      eventTag: 'delete',
      label: t('common.delete'),
      danger: true,
      permission: ['PROJECT_API_DEFINITION_MOCK:READ+DELETE'],
    },
  ];

  const tableQueryParams = ref<any>();

  async function getModuleIds() {
    let moduleIds: string[] = [];
    if (props.activeModule !== 'all') {
      moduleIds = [props.activeModule];
      const getAllChildren = await tableStore.getSubShow(TableKeyEnum.API_TEST_MANAGEMENT_MOCK);
      if (getAllChildren) {
        moduleIds = [props.activeModule, ...props.offspringIds];
      }
    }
    return moduleIds;
  }

  async function loadMockList() {
    const selectModules = await getModuleIds();
    const params = {
      keyword: keyword.value,
      projectId: appStore.currentProjectId,
      protocol: props.protocol,
      apiDefinitionId: props.definitionDetail.id !== 'all' ? props.definitionDetail.id : undefined,
      filter: {},
      moduleIds: selectModules,
    };
    setLoadListParams(params);
    loadList();
    tableQueryParams.value = {
      ...params,
      current: propsRes.value.msPagination?.current,
      pageSize: propsRes.value.msPagination?.pageSize,
    };
    emit('init', {
      ...tableQueryParams.value,
    });
  }

  watchEffect(() => {
    if (props.activeModule || props.protocol) {
      loadMockList();
    }
  });

  async function handleBeforeEnableChange(record: ApiDefinitionMockDetail) {
    try {
      await updateMockStatusPage(record.id);
      Message.success(record.enable ? t('common.disableSuccess') : t('common.enableSuccess'));
      return true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
      return false;
    }
  }

  const tableSelected = ref<(string | number)[]>([]);
  const batchParams = ref<BatchActionQueryParams>({
    selectedIds: [],
    selectAll: false,
    excludeIds: [],
    currentSelectCount: 0,
  });

  /**
   * 删除接口
   */
  function removeMock(record?: ApiDefinitionMockDetail, isBatch?: boolean, params?: BatchActionQueryParams) {
    let title = t('apiTestManagement.confirmDelete', { name: record?.name });
    let selectIds = [record?.id || ''];
    if (isBatch) {
      title = t('mockManagement.batchDeleteMockTip', {
        count: params?.currentSelectCount || tableSelected.value.length,
      });
      selectIds = tableSelected.value as string[];
    }
    openModal({
      type: 'error',
      title,
      content: t('apiTestManagement.deleteMockTip'),
      okText: t('common.confirmDelete'),
      cancelText: t('common.cancel'),
      okButtonProps: {
        status: 'danger',
      },
      maskClosable: false,
      onBeforeOk: async () => {
        try {
          if (isBatch) {
            const selectModules = await getModuleIds();
            await batchDeleteMock({
              selectIds,
              selectAll: !!params?.selectAll,
              excludeIds: params?.excludeIds || [],
              condition: { keyword: keyword.value },
              projectId: appStore.currentProjectId,
              moduleIds: selectModules,
            });
          } else {
            await deleteMock({
              id: record?.id as string,
              projectId: appStore.currentProjectId,
            });
          }
          Message.success(t('common.deleteSuccess'));
          resetSelector();
          loadMockList();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        }
      },
      hideCancel: false,
    });
  }

  const { copy, isSupported } = useClipboard({ legacy: true });

  async function copyMockUrl(record: ApiDefinitionMockDetail) {
    try {
      appStore.showLoading();
      const url = await getMockUrl(record.id);
      if (isSupported) {
        copy(url);
        Message.success(t('common.copySuccess'));
      } else {
        Message.warning(t('common.copyNotSupport'));
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      appStore.hideLoading();
    }
  }

  /**
   * 处理表格更多按钮事件
   * @param item
   */
  function handleTableMoreActionSelect(item: ActionsItem, record: ApiDefinitionMockDetail) {
    switch (item.eventTag) {
      case 'delete':
        removeMock(record);
        break;
      case 'copyMock':
        copyMockUrl(record);
        break;
      default:
        break;
    }
  }

  /**
   * 处理表格选中
   */
  function handleTableSelect(arr: (string | number)[]) {
    tableSelected.value = arr;
  }

  const showBatchModal = ref(false);
  const batchUpdateLoading = ref(false);
  const batchFormRef = ref<FormInstance>();
  const batchForm = ref({
    attr: 'Status' as 'Status' | 'Tags',
    value: true,
    values: [],
    append: false,
  });
  const fullAttrs = [
    {
      name: 'common.status',
      value: 'Status',
    },
    {
      name: 'common.tag',
      value: 'Tags',
    },
  ];
  const attrOptions = computed(() => {
    if (props.protocol === 'HTTP') {
      return fullAttrs;
    }
    return fullAttrs.filter((e) => e.value !== 'method');
  });

  function cancelBatch() {
    showBatchModal.value = false;
    batchFormRef.value?.resetFields();
    batchForm.value = {
      attr: 'Status',
      value: true,
      values: [],
      append: false,
    };
  }

  function batchUpdate() {
    batchFormRef.value?.validate(async (errors) => {
      if (!errors) {
        try {
          batchUpdateLoading.value = true;
          await batchEditMock({
            selectIds: batchParams.value?.selectedIds || [],
            selectAll: !!batchParams.value?.selectAll,
            excludeIds: batchParams.value?.excludeIds || [],
            condition: {
              keyword: keyword.value,
            },
            projectId: appStore.currentProjectId,
            moduleIds: await getModuleIds(),
            type: batchForm.value.attr,
            append: batchForm.value.append,
            tags: batchForm.value.attr === 'Tags' ? batchForm.value.values : [],
            enable: batchForm.value.attr === 'Status' ? batchForm.value.value : false,
          });
          Message.success(t('common.updateSuccess'));
          cancelBatch();
          resetSelector();
          loadMockList();
        } catch (error) {
          // eslint-disable-next-line no-console
          console.log(error);
        } finally {
          batchUpdateLoading.value = false;
        }
      }
    });
  }

  /**
   * 处理表格选中后批量操作
   * @param event 批量操作事件对象
   */
  function handleTableBatch(event: BatchActionParams, params: BatchActionQueryParams) {
    tableSelected.value = params?.selectedIds || [];
    batchParams.value = params;
    switch (event.eventTag) {
      case 'delete':
        removeMock(undefined, true, params);
        break;
      case 'edit':
        showBatchModal.value = true;
        break;
      default:
        break;
    }
  }

  const mockDetailDrawerVisible = ref(false);
  const activeMockRecord = ref<ApiDefinitionMockDetail>();
  const isCopy = ref(false);
  const isEdit = ref(false);

  function createMock() {
    activeMockRecord.value = undefined;
    isCopy.value = false;
    isEdit.value = false;
    mockDetailDrawerVisible.value = true;
  }

  const mockBelongDefinitionDetail = ref<RequestParam>(props.definitionDetail);
  async function openMockDetailDrawer(record: ApiDefinitionMockDetail) {
    try {
      activeMockRecord.value = record;
      if (props.definitionDetail.id === 'all') {
        // 从全部 mock 列表页查看 mock 详情，需要先加载其接口定义详情
        appStore.showLoading();
        const res = await getDefinitionDetail(record.apiDefinitionId);
        mockBelongDefinitionDetail.value = {
          ...(res.request as RequestParam),
          id: res.id,
          type: 'mock',
          isNew: false,
          isCopy: false,
          protocol: res.protocol,
          activeTab: RequestComposition.BODY,
          executeLoading: false,
          responseDefinition: res.response,
        };
      } else {
        mockBelongDefinitionDetail.value = props.definitionDetail;
      }
      mockDetailDrawerVisible.value = true;
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      appStore.hideLoading();
    }
  }

  function handleOpenDetail(record: ApiDefinitionMockDetail) {
    isEdit.value = false;
    isCopy.value = false;
    openMockDetailDrawer(record);
  }

  function handleCopyMock(record: ApiDefinitionMockDetail) {
    isCopy.value = true;
    isEdit.value = false;
    openMockDetailDrawer(record);
  }

  function editMock(record: ApiDefinitionMockDetail) {
    isEdit.value = true;
    isCopy.value = false;
    openMockDetailDrawer(record);
  }

  async function debugMock(record: ApiDefinitionMockDetail) {
    try {
      appStore.showLoading();
      const res = await getMockDetail({
        id: record.id,
        projectId: appStore.currentProjectId,
      });
      emit('debug', res);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      appStore.hideLoading();
    }
  }

  defineExpose({
    loadMockList,
  });

  if (!props.readOnly) {
    await tableStore.initColumn(TableKeyEnum.API_TEST_MANAGEMENT_MOCK, columns, 'drawer');
  } else {
    columns = columns.filter(
      (item) => !['version', 'createTime', 'updateTime', 'operation'].includes(item.dataIndex as string)
    );
  }
</script>

<style lang="less" scoped>
  :deep(.param-input:not(.arco-input-focus, .arco-select-view-focus)) {
    &:not(:hover) {
      border-color: transparent !important;
      .arco-input::placeholder {
        @apply invisible;
      }
      .arco-select-view-icon {
        @apply invisible;
      }
      .arco-select-view-value {
        color: var(--color-text-brand);
      }
    }
  }
</style>
