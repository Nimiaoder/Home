<script setup>
import { reactive, ref } from 'vue'
import BaseModal from '@/components/ui/BaseModal.vue'
import VersionPicker from './VersionPicker.vue'
import JavaRequirementNotice from './JavaRequirementNotice.vue'
import { mcServerApi } from '@/api/minecraft'
import { toast } from '@/utils/toast'

const props = defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'created'])

const form = reactive({ name: '', description: '', port: null, memoryGb: 2, acceptEula: false })
const pick = ref({ type: 'PAPER', mcVersion: '', build: '' })
const requiredJava = ref(0)
const saving = ref(false)

function submit() {
  if (!form.name.trim()) return toast.error('請輸入伺服器名稱')
  if (!pick.value.mcVersion) return toast.error('請選擇 Minecraft 版本')
  saving.value = true
  mcServerApi.create(
    {
      name: form.name.trim(),
      description: form.description.trim(),
      type: pick.value.type,
      mcVersion: pick.value.mcVersion,
      build: pick.value.build,
      port: form.port || null,
      memoryMb: Math.round(form.memoryGb * 1024),
      acceptEula: form.acceptEula
    },
    (server) => {
      emit('update:modelValue', false)
      emit('created', server)
    },
    { showSuccess: true, onFinally: () => (saving.value = false) }
  )
}
</script>

<template>
  <BaseModal :model-value="modelValue" title="新增 Minecraft 伺服器" width="640px" :persistent="saving"
             @update:model-value="emit('update:modelValue', $event)">
    <div class="stack">
      <div class="form-grid">
        <div class="form-field">
          <label>伺服器名稱</label>
          <input v-model="form.name" class="input" maxlength="50" placeholder="例如：生存服" />
        </div>
        <div class="form-field">
          <label>連接埠（留空自動選擇）</label>
          <input v-model.number="form.port" class="input" type="number" min="1024" max="65535" placeholder="25565" />
        </div>
      </div>

      <VersionPicker v-model="pick" @java="requiredJava = $event" />
      <JavaRequirementNotice :required="requiredJava" />

      <div class="form-grid">
        <div class="form-field">
          <label>記憶體上限（GB）</label>
          <input v-model.number="form.memoryGb" class="input" type="number" min="0.5" step="0.5" />
          <span class="hint">玩家少約 2 GB；裝模組建議 4 GB 以上。</span>
        </div>
        <div class="form-field">
          <label>說明（選填）</label>
          <input v-model="form.description" class="input" maxlength="255" />
        </div>
      </div>

      <label class="check">
        <input v-model="form.acceptEula" type="checkbox" />
        <span>我同意 <a href="https://aka.ms/MinecraftEULA" target="_blank" rel="noopener" class="lnk">Minecraft EULA</a>（不勾選也可以，之後在「總覽」再同意）</span>
      </label>
    </div>
    <template #footer>
      <button class="btn ghost sm" :disabled="saving" @click="emit('update:modelValue', false)">取消</button>
      <button class="btn sm" :disabled="saving" @click="submit">{{ saving ? '建立中…' : '建立並安裝' }}</button>
    </template>
  </BaseModal>
</template>

<style scoped>
.lnk { color: var(--accent); text-decoration: underline; }
</style>
