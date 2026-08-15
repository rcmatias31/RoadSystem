# Refatoração para Sincronização Direta com Google Sheets API

Este plano descreve a refatoração do sistema de cadastro de novos clientes para utilizar a Google Sheets API diretamente, eliminando a dependência do endpoint REST intermediário que está retornando erro 404.

## User Review Required

> [!IMPORTANT]
> A implementação assume que o arquivo de credenciais da Service Account (`service_account.json`) deve estar presente na pasta `app/src/main/assets/`. Caso o nome ou local do arquivo seja outro, favor informar.
>
> Também é necessário confirmar o **Spreadsheet ID** da planilha de destino. Usarei um placeholder que deverá ser substituído pelo ID real.

## Proposed Changes

### [Dependências]

#### [MODIFY] [libs.versions.toml](file:///C:/Users/rcmat/AndroidStudioProjects/RoadSystem/gradle/libs.versions.toml)
Adicionar as bibliotecas cliente da Google API e autenticação.

#### [MODIFY] [build.gradle.kts](file:///C:/Users/rcmat/AndroidStudioProjects/RoadSystem/app/build.gradle.kts)
Implementar as novas dependências e configurar as exclusões de arquivos de recursos conflitantes (META-INF).

---

### [Injeção de Dependência]

#### [NEW] [GoogleSheetsModule.kt](file:///C:/Users/rcmat/AndroidStudioProjects/RoadSystem/app/src/main/java/com/raphael/roadsystem/core/di/GoogleSheetsModule.kt)
Configurar o `Sheets` service utilizando a Service Account e expor o `spreadsheetId`.

#### [MODIFY] [DataModule.kt](file:///C:/Users/rcmat/AndroidStudioProjects/RoadSystem/app/src/main/java/com/raphael/roadsystem/core/di/DataModule.kt)
Atualizar o `provideSheetsRepository` para injetar o `Sheets` service e o `spreadsheetId`.

---

### [Repositório e Sincronização]

#### [MODIFY] [SheetsRepository.kt](file:///C:/Users/rcmat/AndroidStudioProjects/RoadSystem/app/src/main/java/com/raphael/roadsystem/data/SheetsRepository.kt)
- Adicionar o método `appendNovoClientePlanilha` utilizando o `sheetsService`.
- Atualizar o construtor para receber as novas dependências.

#### [MODIFY] [SyncNovoClienteWorker.kt](file:///C:/Users/rcmat/AndroidStudioProjects/RoadSystem/app/src/main/java/com/raphael/roadsystem/sync/SyncNovoClienteWorker.kt)
- Refatorar para chamar `repository.appendNovoClientePlanilha`.
- Remover a dependência da `RoadSystemApi` para esta operação.

## Verification Plan

### Automated Tests
- Executar `gradlew app:assembleDebug` para garantir que as novas dependências não geram conflitos de build.

### Manual Verification
- Cadastrar um novo cliente no app.
- Verificar se o `SyncNovoClienteWorker` é disparado.
- Validar se os dados aparecem na planilha Google Sheets na aba "Clientes".
