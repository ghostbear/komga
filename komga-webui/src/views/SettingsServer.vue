<template>
  <v-container fluid class="pa-6">
    <v-row>
      <v-col><span class="text-h5">{{ $t('server.server_management.section_title') }}</span></v-col>
    </v-row>
    <v-row>
      <v-col>
        <v-btn @click="confirmEmptyTrash = true"
               style="margin-right: 15px"
               color="error"
        >{{ $t('server.server_management.button_empty_trash') }}
        </v-btn>
        <v-btn @click="modalStopServer = true"
               color="error"
        >{{ $t('server.server_management.button_shutdown') }}
        </v-btn>
      </v-col>
    </v-row>

    <confirmation-dialog
      v-model="confirmEmptyTrash"
      :title="$t('dialog.empty_trash.title')"
      :body="$t('dialog.empty_trash.body')"
      :button-confirm="$t('dialog.empty_trash.button_confirm')"
      @confirm="emptyTrash"
    />

    <confirmation-dialog
      v-model="modalStopServer"
      :title="$t('dialog.server_stop.dialog_title')"
      :body="$t('dialog.server_stop.confirmation_message')"
      :button-confirm="$t('dialog.server_stop.button_confirm')"
      button-confirm-color="error"
      @confirm="stopServer"
    />

  </v-container>
</template>

<script lang="ts">
import Vue from 'vue'
import ConfirmationDialog from '@/components/dialogs/ConfirmationDialog.vue'
import {ERROR} from '@/types/events'
import {LibraryDto} from '@/types/komga-libraries'

export default Vue.extend({
  name: 'SettingsServer',
  components: {ConfirmationDialog},
  data: () => ({
    modalStopServer: false,
    confirmEmptyTrash: false,
  }),
  computed: {
    libraries(): LibraryDto[] {
      return this.$store.state.komgaLibraries.libraries
    },
  },
  methods: {
    emptyTrash() {
      this.libraries.forEach(library => {
        this.$komgaLibraries.emptyTrash(library)
      })
    },
    async stopServer() {
      try {
        await this.$actuator.shutdown()
      } catch (e) {
        this.$eventHub.$emit(ERROR, {message: e.message} as ErrorEvent)
      }
    },
  },
})
</script>

<style scoped>

</style>
