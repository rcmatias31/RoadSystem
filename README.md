# RoadSystem 🚚

RoadSystem é um aplicativo de logística inteligente desenvolvido com as tecnologias mais modernas do ecossistema Android. Ele foi projetado para motoristas que precisam de roteirização otimizada, rastreamento em tempo real e operação resiliente mesmo em condições de baixa conectividade.

## 🚀 Funcionalidades (MVP)

1.  **Autenticação Segura**: Login social com Google via Firebase Auth e Credential Manager.
2.  **Rastreamento em Tempo Real**: Monitoramento constante da posição do motorista no mapa utilizando `FusedLocationProviderClient`.
3.  **Gestão de Perfil (Base)**: Cadastro de endereço residencial com geocodificação automática (converte endereço em Latitude/Longitude) para definir o destino final de todas as rotas.
4.  **Seleção Inteligente de Rotas**: Interface para seleção múltipla de clientes pendentes vindos de uma planilha integrada via API.
5.  **Roteirização Otimizada**: Cálculo automático de rota utilizando a **Google Directions API** com parâmetro `optimize:true`. A rota conecta: **Localização Atual -> Clientes (Waypoints) -> Residência**.
6.  **Navegação Externa**: Integração nativa com Waze e Google Maps via Intents para navegação curva-a-curva, economizando recursos do dispositivo.
7.  **Check-in com Geofencing**: Botão único de check-in que valida a proximidade do motorista ao cliente.
    *   **Presencial**: Validado automaticamente se a distância for ≤ 150 metros.
    *   **Remoto**: Solicita confirmação de segurança via diálogo se o motorista estiver fora do raio de proximidade.
8.  **Arquitetura Offline-First**: Sistema "Store and Forward" que utiliza **Room** e **WorkManager**. Check-ins feitos offline são armazenados localmente e sincronizados automaticamente com o backend assim que a conexão é restabelecida.
9.  **UI/UX Moderna**: Interface construída 100% em **Jetpack Compose** com animações fluidas (`AnimatedContent` e `AnimateContentSize`) para transições suaves entre clientes.

## 🛠️ Stack Tecnológica

*   **Linguagem**: Kotlin
*   **UI**: Jetpack Compose
*   **Arquitetura**: MVVM (Model-View-ViewModel)
*   **Injeção de Dependência**: Hilt (Dagger)
*   **Banco de Dados**: Room (Offline Database)
*   **Rede**: Retrofit + OkHttp
*   **Background Tasks**: WorkManager
*   **Maps**: Maps SDK for Android + Maps Compose Utility
*   **Backend**: Firebase Auth + Google Cloud Functions (Planilha como DB)

## 📦 Estrutura do Projeto

*   `ui/`: Telas e componentes Compose (MainScreen, TelaLogin, etc.).
*   `viewmodel/`: Lógica de negócio e gestão de estado.
*   `data/`: Repositórios, DAOs e Entidades do Room.
*   `api/`: Definições do Retrofit e Clientes de API.
*   `sync/`: Workers para sincronização em background.
*   `utils/`: Utilitários de geolocalização e decodificação de polylines.

## ⚙️ Configuração Necessária

Para rodar o projeto, você precisará configurar as seguintes chaves no seu ambiente:

1.  **Google Cloud Console**:
    *   Habilitar *Maps SDK for Android*, *Directions API* e *Geocoding API*.
    *   Criar uma API Key e adicioná-la ao `local.properties` como `MAPS_API_KEY`.
    *   Vincular uma conta de faturamento (Billing) ativa.
2.  **Firebase**:
    *   Adicionar o arquivo `google-services.json` na pasta `/app`.
    *   Habilitar o método de autenticação Google.

---
*Desenvolvido com foco em performance, estabilidade e experiência do usuário final.*
