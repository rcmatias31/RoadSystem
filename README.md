# 🚚 RoadSystem
> **Aplicativo Android moderno corporativo para roteirização inteligente, navegação otimizada e gestão logística com suporte a operações Offline-First.**

O **RoadSystem** é uma ferramenta de ponta a ponta projetada para motoristas e agentes de campo. Ele resolve o "Problema do Caixeiro Viajante" no dia a dia da logística, permitindo visualização de rotas otimizadas, check-ins inteligentes baseados em geolocalização e funcionamento contínuo mesmo em áreas sem cobertura de internet.

## 🎯 Visão Geral do Fluxo
1. **Autenticação Segura:** Login via Firebase Auth protegido por JWT.
2. **Roteamento Inteligente:** Consumo de rotas via API integradas à **Google Directions API**.
3. **Navegação Externa:** Delegação do trajeto para o Waze / Google Maps nativo.
4. **Check-in Georreferenciado:** Cálculo em tempo real para check-ins presenciais (<= 150m) ou remotos.
5. **Retorno Automático:** UX guiada de volta à base após a última entrega.

## 🛠️ Arquitetura e Tecnologias
Construído com **Clean Architecture** e **MVVM**.
* **Linguagem:** Kotlin
* **UI:** Jetpack Compose (Material 3)
* **Injeção de Dependências:** Dagger-Hilt
* **Cache Offline:** Room Database
* **Sincronização:** WorkManager
* **Rede:** Retrofit + OkHttp

## 📡 Engenharia Offline-First
* **Optimistic UI:** Interface updated instantly.
* **Store and Forward:** Data recorded locally (Room) and synchronized in the background (WorkManager) when connection is restored.

## ✅ Últimas Implementações (Fase 1 e Refinamentos)

- **Mapeamento Dinâmico (Google Sheets API):** Implementada a leitura do range `A2:G`, com suporte automático a categorias (Coluna G). O app agora gera **FilterChips dinâmicos** baseados no conteúdo da planilha.
- **Resiliência Geográfica (GeoUtils):** Desenvolvido algoritmo de parsing robusto para tratar coordenadas em formatos inconsistentes (notação científica como `E7` ou falta de ponto decimal/microdegrees). O sistema valida e corrige a escala para a região geográfica do Brasil automaticamente.
- **Persistência de Estado (Rota Blindada):** A rota ativa agora é persistida no banco de dados local (`rota_ativa`). Isso impede a perda de dados caso o sistema encerre o app enquanto o motorista utiliza o Google Maps para navegação.
- **Lookup Reativo por ID:** Refatoração da arquitetura para utilizar o **ID do cliente** como chave mestre. Mapa e Cards buscam informações em tempo real do banco de dados, garantindo integridade de nomes e endereços.
- **Sincronização Inteligente (Fase 4):** Ajustada a lógica de sync para não limpar o cache local se houver uma jornada em andamento. Implementado **WorkManager com Backoff Exponencial** e auditoria de GPS (latitude/longitude gravadas no momento do clique no botão).
- **Dashboard e Histórico (Fase 5):** 
    - **Histórico Persistente de 30 dias:** Atendimentos salvos localmente com autolimpeza programada.
    - **Consulta por Calendário:** Interface com `DatePicker` para visualização de entregas em datas anteriores.
    - **Painel de Conclusão:** Resumo automático de atendimentos ao final da rota.
    - **Retorno Inteligente à Base:** Botão que calcula e inicia a navegação GPS para o endereço residencial do motorista.
- **Interface Adaptativa e Responsiva:** Refatoração do layout para suporte a diversos tamanhos de smartphones. Implementada gestão de constraints de tela, tratamento de textos longos (Ellipsis) e ajustes de insets para uma experiência Edge-to-Edge fluida.
- **Filtros e Gestão de Campo (Novas Funcionalidades):**
    - **Filtros Personalizados:** Criação de grupos de clientes com nomes e cores customizáveis diretamente pelo motorista.
    - **Cadastro via GPS:** Registro de novos clientes no mapa e na planilha usando a localização atual, com geocodificação reversa para preenchimento automático de endereço.
    - **Temas Dinâmicos:** Configuração de tema (Claro, Escuro ou Sistema) no perfil do usuário.
    - **Geomarcação Colorida:** Visualização de marcadores no mapa com as cores atribuídas aos filtros/categorias (Geomarking Enabled).
- **Segurança e Build:** Configuração de build variants (Release/Debug) e tratamento automático de caracteres de escape em caminhos de arquivo no `local.properties`.