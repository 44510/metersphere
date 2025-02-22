<template>
  <MsCard
    has-breadcrumb
    :title="title"
    :loading="loading"
    :is-edit="isEdit"
    @save="saveHandler(false)"
    @save-and-continue="saveHandler(true)"
  >
    <template v-if="!isEdit" #headerRight>
      <a-select
        v-model="form.templateId"
        class="w-[240px]"
        :options="templateOption"
        allow-search
        :placeholder="t('bugManagement.edit.defaultSystemTemplate')"
        @change="templateChange"
      />
    </template>
    <a-form ref="formRef" :model="form" layout="vertical">
      <div class="flex flex-row">
        <div class="left mt-[16px] w-[calc(100%-428px)] grow">
          <!-- 平台默认模板不展示缺陷名称, 描述 -->
          <a-form-item
            v-if="!isPlatformDefaultTemplate"
            field="title"
            :label="t('bugManagement.bugName')"
            :rules="[{ required: true, message: t('bugManagement.edit.nameIsRequired') }]"
            :placeholder="t('bugManagement.edit.pleaseInputBugName')"
          >
            <a-input v-model="form.title" :max-length="255" />
          </a-form-item>
          <a-form-item v-if="!isPlatformDefaultTemplate" field="description" :label="t('bugManagement.edit.content')">
            <MsRichText
              v-model:raw="form.description"
              v-model:filed-ids="descriptionFileIds"
              :upload-image="handleUploadImage"
              :preview-url="EditorPreviewFileUrl"
            />
          </a-form-item>
          <!-- 平台默认模板展示字段, 暂时支持输入框, 富文本类型   -->
          <div v-if="isPlatformDefaultTemplate">
            <a-form-item
              v-for="(value, key) in form.platformSystemFields"
              :key="key"
              :field="'platformSystemFields.' + key"
              :label="platformSystemFieldMap[key].fieldName"
              :rules="[
                {
                  required: platformSystemFieldMap[key].required,
                  message: `${platformSystemFieldMap[key].fieldName}` + t('bugManagement.edit.cannotBeNull'),
                },
              ]"
            >
              <a-input
                v-if="platformSystemFieldMap[key].type === 'INPUT'"
                v-model="form.platformSystemFields[key]"
                :max-length="255"
              />
              <MsRichText
                v-if="platformSystemFieldMap[key].type === 'RICH_TEXT'"
                v-model:raw="form.platformSystemFields[key]"
                v-model:filed-ids="descriptionFileIdMap[key]"
                :upload-image="handleUploadImage"
                :preview-url="EditorPreviewFileUrl"
              />
            </a-form-item>
          </div>
          <a-form-item field="attachment">
            <div class="flex flex-col">
              <div class="mb-1">
                <AddAttachment v-model:file-list="fileList" @change="handleChange" @link-file="associatedFile" />
              </div>
            </div>
          </a-form-item>
          <MsFileList
            ref="fileListRef"
            v-model:file-list="fileList"
            :init-file-save-tips="t('ms.upload.waiting_save')"
            mode="static"
          >
            <template #actions="{ item }">
              <!-- 本地文件 -->
              <div v-if="item.local || item.status === 'init'" class="flex flex-nowrap">
                <MsButton
                  v-if="item.status !== 'init' && item.file.type.includes('image')"
                  type="button"
                  status="primary"
                  class="!mr-[4px]"
                  @click="handlePreview(item)"
                >
                  {{ t('ms.upload.preview') }}
                </MsButton>
                <MsButton
                  v-if="item.status !== 'init'"
                  type="button"
                  status="primary"
                  class="!mr-[4px]"
                  @click="transferFile(item)"
                >
                  {{ t('caseManagement.featureCase.storage') }}
                </MsButton>
                <SaveAsFilePopover
                  v-model:visible="transferVisible"
                  :saving-file="activeTransferFileParams"
                  :file-save-as-source-id="(form.id as string)"
                  :file-save-as-api="transferFileRequest"
                  :file-module-options-api="getTransferFileTree"
                  source-id-key="bugId"
                  @finish="getDetailInfo()"
                />
                <MsButton
                  v-if="item.status !== 'init'"
                  type="button"
                  status="primary"
                  class="!mr-[4px]"
                  @click="downloadFile(item)"
                >
                  {{ t('common.download') }}
                </MsButton>
              </div>
              <!-- 关联文件 -->
              <div v-else class="flex flex-nowrap">
                <MsButton
                  v-if="item.status !== 'init' && item.file.type.includes('image')"
                  type="button"
                  status="primary"
                  class="!mr-[4px]"
                  @click="handlePreview(item)"
                >
                  {{ t('ms.upload.preview') }}
                </MsButton>
                <MsButton v-if="bugId" type="button" status="primary" class="!mr-[4px]" @click="downloadFile(item)">
                  {{ t('common.download') }}
                </MsButton>
                <MsButton
                  v-if="bugId && item.isUpdateFlag"
                  type="button"
                  status="primary"
                  @click="handleUpdateFile(item)"
                >
                  {{ t('common.update') }}
                </MsButton>
              </div>
            </template>
            <template #title="{ item }">
              <span v-if="item.isUpdateFlag" class="ml-4 flex items-center font-normal text-[rgb(var(--warning-6))]"
                ><icon-exclamation-circle-fill /> <span>{{ t('caseManagement.featureCase.fileIsUpdated') }}</span>
              </span>
            </template>
          </MsFileList>
        </div>
        <a-divider class="ml-[16px]" direction="vertical" />
        <div class="right mt-[16px] w-[428px] grow pr-[24px]">
          <div class="min-w-[250px] overflow-auto">
            <a-skeleton v-if="isLoading" :loading="isLoading" :animation="true">
              <a-space direction="vertical" class="w-full" size="large">
                <a-skeleton-line :rows="12" :line-height="30" :line-spacing="30" />
              </a-space>
            </a-skeleton>
            <a-form v-else :model="form" layout="vertical">
              <div style="display: inline-block; width: 100%; word-wrap: break-word">
                <MsFormCreate
                  ref="formCreateRef"
                  v-model:formItem="formItem"
                  v-model:api="fApi"
                  :form-rule="formRules"
                />
              </div>

              <a-form-item v-if="!isPlatformDefaultTemplate" field="tag" :label="t('bugManagement.tag')">
                <MsTagsInput
                  v-model:model-value="form.tags"
                  :placeholder="t('bugManagement.edit.tagPlaceholder')"
                  allow-clear
                />
              </a-form-item>
            </a-form>
          </div>
        </div>
      </div>
    </a-form>
  </MsCard>
  <div>
    <MsUpload
      v-model:file-list="fileList"
      accept="none"
      :auto-upload="false"
      :sub-text="acceptType === 'jar' ? '' : t('project.fileManagement.normalFileSubText', { size: 50 })"
      multiple
      draggable
      size-unit="MB"
      :max-size="50"
      :is-all-screen="true"
      class="mb-[16px]"
      @change="handleChange"
    />
  </div>
  <RelateFileDrawer
    v-model:visible="associatedDrawer"
    :get-tree-request="getModules"
    :get-count-request="getModulesCount"
    :get-list-request="getAssociatedFileList"
    :get-list-fun-params="getListFunParams"
    @save="saveSelectAssociatedFile"
  />
  <a-image-preview v-model:visible="previewVisible" :src="imageUrl" />
</template>

<script setup lang="ts">
  import { useRoute } from 'vue-router';
  import { Message } from '@arco-design/web-vue';

  import MsButton from '@/components/pure/ms-button/index.vue';
  import MsCard from '@/components/pure/ms-card/index.vue';
  import MsFormCreate from '@/components/pure/ms-form-create/ms-form-create.vue';
  import { FormItem, FormRuleItem } from '@/components/pure/ms-form-create/types';
  import MsRichText from '@/components/pure/ms-rich-text/MsRichText.vue';
  import MsTagsInput from '@/components/pure/ms-tags-input/index.vue';
  import MsFileList from '@/components/pure/ms-upload/fileList.vue';
  import MsUpload from '@/components/pure/ms-upload/index.vue';
  import { MsFileItem } from '@/components/pure/ms-upload/types';
  import AddAttachment from '@/components/business/ms-add-attachment/index.vue';
  import SaveAsFilePopover from '@/components/business/ms-add-attachment/saveAsFilePopover.vue';
  import RelateFileDrawer from '@/components/business/ms-link-file/associatedFileDrawer.vue';

  import {
    checkFileIsUpdateRequest,
    createOrUpdateBug,
    downloadFileRequest,
    editorUploadFile,
    getAssociatedFileList,
    getBugDetail,
    getTemplateById,
    getTemplateOption,
    previewFile,
    transferFileRequest,
    updateFile,
  } from '@/api/modules/bug-management';
  import { getTransferFileTree } from '@/api/modules/case-management/featureCase';
  import { getModules, getModulesCount } from '@/api/modules/project-management/fileManagement';
  import { EditorPreviewFileUrl } from '@/api/requrls/bug-management';
  import { useI18n } from '@/hooks/useI18n';
  import useLeaveUnSaveTip from '@/hooks/useLeaveUnSaveTip';
  import useVisit from '@/hooks/useVisit';
  import router from '@/router';
  import { useAppStore } from '@/store';
  import useUserStore from '@/store/modules/user';
  import { downloadByteFile } from '@/utils';
  import { scrollIntoView } from '@/utils/dom';
  import { findParents, Option } from '@/utils/recursion';

  import type { CustomFieldItem } from '@/models/bug-management';
  import {
    BugEditCustomField,
    BugEditCustomFieldItem,
    BugEditFormObject,
    BugTemplateRequest,
  } from '@/models/bug-management';
  import { AssociatedList, AttachFileInfo } from '@/models/caseManagement/featureCase';
  import { TableQueryParams } from '@/models/common';
  import { SelectValue } from '@/models/projectManagement/menuManagement';
  import { BugManagementRouteEnum } from '@/enums/routeEnum';

  import { convertToFile } from '../case-management/caseManagementFeature/components/utils';
  import { convertToFileByBug } from './utils';

  defineOptions({ name: 'BugEditPage' });
  const { setIsSave } = useLeaveUnSaveTip();
  setIsSave(false);

  const { t } = useI18n();
  interface TemplateOption {
    label: string;
    value: string;
  }

  const appStore = useAppStore();
  const route = useRoute();
  const templateOption = ref<TemplateOption[]>([]);
  const form = ref<BugEditFormObject>({
    projectId: appStore.currentProjectId,
    title: '',
    description: '',
    templateId: '',
    tags: [],
    deleteLocalFileIds: [],
    unLinkRefIds: [],
    linkFileIds: [],
    // 平台默认模板系统字段
    platformSystemFields: {},
  });

  // 平台默认模板系统字段
  const platformSystemFieldMap: Record<string, any> = {};

  const getListFunParams = ref<TableQueryParams>({
    combine: {
      hiddenIds: [],
    },
  });
  const formRef = ref();
  const formCreateRef = ref();
  // 保存后端传进来的文件(初始状态)
  const attachmentsList = ref<AttachFileInfo[]>([]);
  // 前端展示的文件(当前状态)
  const fileList = ref<MsFileItem[]>([]);
  const formRules = ref<FormItem[]>([]);
  const formItem = ref<FormRuleItem[]>([]);
  const fApi = ref<any>(null);
  const currentProjectId = computed(() => appStore.currentProjectId);
  const associatedDrawer = ref(false);
  const loading = ref(false);
  const acceptType = ref('none'); // 模块-上传文件类型
  const userStore = useUserStore();
  const isEdit = computed(() => !!route.query.id && route.params.mode === 'edit');
  const bugId = computed(() => route.query.id || '');
  const isEditOrCopy = computed(() => !!bugId.value);
  const isCopy = computed(() => route.params.mode === 'copy');
  const isPlatformDefaultTemplate = ref(false);
  const imageUrl = ref('');
  const previewVisible = ref<boolean>(false);
  // 内容/富文本临时附件ID
  const descriptionFileIds = ref<string[]>([]);
  // 描述-环境/富文本临时附件ID
  const descriptionFileIdMap = ref<Record<string, string[]>>({});
  const visitedKey = 'doNotNextTipCreateBug';
  const { getIsVisited } = useVisit(visitedKey);

  const title = computed(() => {
    if (isCopy.value) {
      return t('bugManagement.copyBug');
    }
    return isEdit.value ? t('bugManagement.editBug') : t('bugManagement.createBug');
  });
  const isLoading = ref<boolean>(true);
  const rowLength = ref<number>(0);

  // 处理文件参数
  function getFilesParams() {
    // link file
    const associateFileIds = attachmentsList.value.filter((item) => !item.local).map((item) => item.fileId);
    form.value.linkFileIds = fileList.value
      .filter((item) => !item.local && !item.isCopyFlag && !associateFileIds.includes(item.uid))
      .map((item) => item.uid);
    // unlink file
    const remainLinkFileIds = fileList.value.filter((item) => !item.local && !item.isCopyFlag).map((item) => item.uid);
    form.value.unLinkRefIds = attachmentsList.value
      .filter((item) => !item.local && !remainLinkFileIds.includes(item.fileId))
      .map((item) => item.refId);
    // delete local file
    const remainLocalFileIds = fileList.value
      .filter((item) => item.local && !item.isCopyFlag && item.status !== 'init')
      .map((item) => item.uid);
    form.value.deleteLocalFileIds = attachmentsList.value
      .filter((item) => item.local && !remainLocalFileIds.includes(item.fileId))
      .map((item) => item.fileId);
  }

  // 监视文件列表处理关联和本地文件
  watch(
    () => fileList.value,
    (val) => {
      if (val) {
        getListFunParams.value.combine.hiddenIds = fileList.value.filter((item) => !item.local).map((item) => item.uid);
        getFilesParams();
      }
    },
    { deep: true }
  );

  const transferVisible = ref<boolean>(false);
  const activeTransferFileParams = ref<MsFileItem>();
  // 转存
  function transferFile(item: MsFileItem) {
    activeTransferFileParams.value = {
      ...item,
    };
    transferVisible.value = true;
  }

  // 处理表单格式
  const getFormRules = (arr: BugEditCustomField[]) => {
    formRules.value = [];
    const memberType = ['MEMBER', 'MULTIPLE_MEMBER'];

    if (Array.isArray(arr) && arr.length) {
      formRules.value = arr.map((item: any) => {
        let initValue = item.defaultValue;
        const initOptions = item.options ? item.options : JSON.parse(item.platformOptionJson);
        if (memberType.includes(item.type)) {
          if (item.defaultValue === 'CREATE_USER' || item.defaultValue.includes('CREATE_USER')) {
            initValue = item.type === 'MEMBER' ? userStore.id : [userStore.id];
          }
        }
        return {
          type: item.type,
          name: item.fieldId,
          label: item.fieldName,
          value: initValue,
          options: initOptions,
          required: item.required as boolean,
          platformPlaceHolder: item.platformPlaceHolder,
          props: {
            modelValue: initValue,
            options: initOptions,
          },
        };
      });
    }
  };
  const currentCustomFields = ref<CustomFieldItem[]>([]);
  // TODO:: Record<string, any>
  const templateChange = async (v: SelectValue, request?: BugTemplateRequest) => {
    if (v) {
      try {
        isLoading.value = true;
        let param = { projectId: appStore.currentProjectId, id: v };
        if (request) {
          param = { ...param, ...request };
        }
        const res = await getTemplateById(param);
        currentCustomFields.value = res.customFields || [];
        await getFormRules(res.customFields);
        isLoading.value = false;
        isPlatformDefaultTemplate.value = res.platformDefault;
        if (isPlatformDefaultTemplate.value) {
          const systemFields = res.customFields.filter((field: Record<string, any>) => field.platformSystemField);
          systemFields.forEach((field: Record<string, any>) => {
            form.value.platformSystemFields[field.fieldId] = field.defaultValue;
            platformSystemFieldMap[field.fieldId] = field;
          });
        }
        getFormRules(res.customFields.filter((field: Record<string, any>) => !field.platformSystemField));
      } catch (error) {
        // eslint-disable-next-line no-console
        console.log(error);
      } finally {
        loading.value = false;
      }
    }
  };

  const getTemplateOptions = async () => {
    try {
      loading.value = true;
      const res = await getTemplateOption(appStore.currentProjectId);
      templateOption.value = res.map((item) => {
        if (item.enableDefault && !isEdit.value) {
          // 当创建时 选中默认模板
          form.value.templateId = item.id;
          templateChange(item.id);
        }
        return {
          label: item.name,
          value: item.id,
        };
      });
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    } finally {
      loading.value = false;
    }
  };

  // 预览图片
  async function handlePreview(item: MsFileItem) {
    try {
      previewVisible.value = true;
      if (item.status !== 'init') {
        const res = await previewFile({
          projectId: currentProjectId.value,
          bugId: bugId.value as string,
          fileId: item.uid,
          associated: !item.local,
        });
        const blob = new Blob([res], { type: 'image/jpeg' });
        imageUrl.value = URL.createObjectURL(blob);
      } else {
        imageUrl.value = item.url || '';
      }
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 下载
  async function downloadFile(item: MsFileItem) {
    try {
      const res = await downloadFileRequest({
        projectId: currentProjectId.value,
        bugId: bugId.value as string,
        fileId: item.uid,
        associated: !item.local,
      });
      downloadByteFile(res, `${item.name}`);
    } catch (error) {
      // eslint-disable-next-line no-console
      console.log(error);
    }
  }

  // 更新文件
  async function handleUpdateFile(item: MsFileItem) {
    try {
      const params = {
        refId: item.associateId,
        associated: !item.local,
        bugId: bugId.value,
        projectId: currentProjectId.value,
      };
      await updateFile(params);
      Message.success(t('common.updateSuccess'));
    } catch (error) {
      console.log(error);
    }
  }

  function associatedFile() {
    associatedDrawer.value = true;
  }

  function handleChange(_fileList: MsFileItem[]) {
    fileList.value = _fileList.map((e) => {
      return {
        ...e,
        enable: true, // 是否启用
      };
    });
  }

  // 处理关联文件
  function saveSelectAssociatedFile(fileData: AssociatedList[]) {
    const fileResultList = fileData.map((fileInfo) => convertToFile(fileInfo));
    fileList.value.push(...fileResultList);
  }

  function getDescriptionFileId() {
    const fileIds = [] as string[];
    Object.keys(descriptionFileIdMap.value).forEach((key) => {
      if (descriptionFileIdMap.value[key].length > 0) {
        fileIds.push(...descriptionFileIdMap.value[key]);
      }
    });
    return fileIds;
  }

  // 保存
  const saveHandler = async (isContinue = false) => {
    formRef.value.validate((error: any) => {
      if (!error) {
        fApi.value.validate(async (valid: any) => {
          if (valid === true) {
            try {
              loading.value = true;
              const customFields: BugEditCustomFieldItem[] = [];
              if (formItem.value && formItem.value.length) {
                formItem.value.forEach((item: FormRuleItem) => {
                  if (item.sourceType === 'CASCADER') {
                    item.value = findParents(item.options as Option[], item.value as string, []) || '';
                  }
                  customFields.push({
                    id: item.field as string,
                    name: item.title as string,
                    type: item.sourceType as string,
                    value: Array.isArray(item.value) ? JSON.stringify(item.value) : (item.value as string),
                  });
                });
              }
              if (isPlatformDefaultTemplate.value && form.value.platformSystemFields) {
                Object.keys(form.value.platformSystemFields).forEach((key) => {
                  customFields.push({
                    id: platformSystemFieldMap[key].fieldId,
                    name: platformSystemFieldMap[key].fieldName,
                    type: platformSystemFieldMap[key].type,
                    value: form.value.platformSystemFields[key],
                  });
                });
                delete form.value.platformSystemFields;
                // 平台默认模板不传递名称, 描述, 标签等参数
                delete form.value.title;
                delete form.value.description;
                delete form.value.tags;
              }
              // 过滤出复制的附件
              const copyFileList = fileList.value.filter((item) => item.isCopyFlag);
              let copyFiles: { refId: string; fileId: string; local: boolean }[] = [];
              if (copyFileList.length > 0) {
                copyFiles = copyFileList.map((file) => {
                  return {
                    refId: file.associateId,
                    fileId: file.uid,
                    local: file.local,
                    bugId: bugId.value as string,
                    fileName: file.name,
                  };
                });
              }

              const tmpObj: BugEditFormObject = {
                ...form.value,
                customFields,
                copyFiles,
                richTextTmpFileIds: isPlatformDefaultTemplate.value ? getDescriptionFileId() : descriptionFileIds.value,
              };
              if (isCopy.value) {
                delete tmpObj.id;
                delete tmpObj.richTextTmpFileIds;
              }
              // 过滤出本地保存的文件
              const localFiles = fileList.value.filter((item) => item.local && item.status === 'init');
              // 执行保存操作
              const res = await createOrUpdateBug({ request: tmpObj, fileList: localFiles as unknown as File[] });
              if (isEdit.value) {
                setIsSave(true);
                Message.success(t('common.updateSuccess'));
                router.push({
                  name: BugManagementRouteEnum.BUG_MANAGEMENT_INDEX,
                });
              } else {
                Message.success(t('common.createSuccess'));
                if (isContinue) {
                  setIsSave(false);
                  // 如果是保存并继续创建
                  const { templateId } = form.value;
                  // 用当前模板初始化自定义字段
                  await templateChange(templateId);
                  form.value = {
                    projectId: appStore.currentProjectId, // 取当前项目id
                    title: '',
                    description: '',
                    templateId,
                    tags: [],
                  };
                  // 清空文件列表
                  fileList.value = [];
                } else {
                  setIsSave(true);
                  // 否则跳转到成功页
                  if (getIsVisited()) {
                    router.push({
                      name: BugManagementRouteEnum.BUG_MANAGEMENT_INDEX,
                    });
                    return;
                  }
                  router.push({
                    name: BugManagementRouteEnum.BUG_MANAGEMENT_CREATE_SUCCESS,
                    query: {
                      ...route.query,
                      id: res.data.id,
                    },
                  });
                }
              }
            } catch (err) {
              // eslint-disable-next-line no-console
              console.log(err);
            } finally {
              loading.value = false;
            }
          }
        });
      }
    });
    scrollIntoView(document.querySelector('.arco-form-item-message'), { block: 'center' });
  };

  const getOptionFromTemplate = (field: CustomFieldItem | undefined) => {
    if (field) {
      return field.options ? field.options : JSON.parse(field.platformOptionJson);
    }
    return [];
  };

  // 获取详情
  const getDetailInfo = async () => {
    loading.value = true;
    const id = route.query.id as string;
    if (!id) return;
    const res = await getBugDetail(id);
    const { customFields, templateId, attachments } = res;
    // 根据模板ID 初始化自定义字段
    if (isCopy.value) {
      // 复制, 只需返回初始状态
      await templateChange(templateId);
    } else {
      await templateChange(templateId, { fromStatusId: res.status, platformBugKey: res.platformBugId });
    }
    if (attachments && attachments.length) {
      if (!isCopy.value) {
        // 非Copy时, 附件列表赋值
        attachmentsList.value = attachments;
      }
      // 检查文件是否有更新
      const checkUpdateFileIds = await checkFileIsUpdateRequest(attachments.map((item: any) => item.fileId));
      // 处理文件列表
      fileList.value = attachments
        .map((fileInfo: any) => {
          return {
            ...fileInfo,
            name: fileInfo.fileName,
            isUpdateFlag: checkUpdateFileIds.includes(fileInfo.fileId),
            isCopyFlag: isCopy.value,
          };
        })
        .map((fileInfo: any) => {
          return convertToFileByBug(fileInfo);
        });
    }

    let tmpObj: Record<string, any> = {};
    if (isEdit.value) {
      tmpObj = { status: res.status };
    }
    if (customFields && Array.isArray(customFields)) {
      const MULTIPLE_TYPE = ['MULTIPLE_SELECT', 'MULTIPLE_INPUT', 'CHECKBOX', 'MULTIPLE_MEMBER'];
      const SINGRADIO_TYPE = ['RADIO', 'SELECT', 'MEMBER'];
      customFields.forEach((item) => {
        if (item.id === 'status' && isCopy.value) {
          // 复制时, 状态赋值为空
          tmpObj[item.id] = '';
          // 多选类型需要过滤选项
        } else if (MULTIPLE_TYPE.includes(item.type)) {
          const multipleOptions = getOptionFromTemplate(
            currentCustomFields.value.find((filed: any) => item.id === filed.fieldId)
          );
          // 如果该值在选项中已经被删除掉
          const optionsIds = (multipleOptions || []).map((e: any) => e.value);
          if (item.type !== 'MULTIPLE_INPUT') {
            if (item.value) {
              const currentDefaultValue = optionsIds.filter((e: any) => JSON.parse(item.value).includes(e));
              tmpObj[item.id] = currentDefaultValue;
            } else {
              tmpObj[item.id] = [];
            }
          } else {
            tmpObj[item.id] = JSON.parse(item.value);
          }
        } else if (item.type === 'INT' || item.type === 'FLOAT') {
          tmpObj[item.id] = Number(item.value);
        } else if (item.type === 'CASCADER') {
          const arr = JSON.parse(item.value);
          if (arr && arr instanceof Array && arr.length > 0) {
            tmpObj[item.id] = arr[arr.length - 1];
          }
          // 单选多选项
        } else if (SINGRADIO_TYPE.includes(item.type)) {
          const multipleOptions = getOptionFromTemplate(
            currentCustomFields.value.find((filed: any) => item.id === filed.fieldId)
          );
          // 如果该值在选项中已经被删除掉
          const optionsIds = (multipleOptions || []).map((e: any) => e.value);
          const currentDefaultValue = optionsIds.find((e: any) => item.value === e) || '';
          tmpObj[item.id] = currentDefaultValue;
        } else {
          tmpObj[item.id] = item.value;
        }
      });
    }
    // 自定义字段赋值
    fApi.value.setValue(tmpObj);
    // 平台默认模板系统字段单独处理
    if (isPlatformDefaultTemplate.value && form.value.platformSystemFields) {
      Object.keys(form.value.platformSystemFields).forEach((key) => {
        form.value.platformSystemFields[key] = tmpObj[key];
      });
    }
    const { platformSystemFields } = form.value;

    let copyName = '';
    if (isCopy.value) {
      copyName = `copy_${res.title}`;
      if (copyName.length > 255) {
        form.value.title = copyName.slice(0, 255);
      }
    }
    // 表单赋值
    form.value = {
      id: res.id,
      title: isCopy.value ? copyName : res.title,
      description: res.description,
      templateId: res.templateId,
      tags: res.tags || [],
      projectId: res.projectId,
      platformSystemFields,
    };
    loading.value = false;
  };

  const initDefaultFields = async () => {
    await getTemplateOptions();
  };

  // 监视自定义字段改变处理formCreate
  watch(
    () => formRules.value,
    () => {
      rowLength.value = formRules.value.length + 2;
    },
    { deep: true }
  );

  // 监视文件列表处理关联和本地文件
  watch(
    () => fileList.value,
    (val) => {
      if (val) {
        getListFunParams.value.combine.hiddenIds = fileList.value.filter((item) => !item.local).map((item) => item.uid);
        if (isEdit.value) {
          getFilesParams();
        }
      }
    },
    { deep: true }
  );

  async function handleUploadImage(file: File) {
    const { data } = await editorUploadFile({
      fileList: [file],
    });
    return data;
  }

  onMounted(async () => {
    await initDefaultFields();
    if (isEditOrCopy.value) {
      // 详情
      await getDetailInfo();
    }
  });
</script>

<style lang="less" scoped>
  :deep(.arco-form-item-extra) {
    font-size: 14px;
    color: var(--color-text-4);
  }
  :deep(.arco-form-item-content) {
    overflow-wrap: anywhere;
  }
</style>
