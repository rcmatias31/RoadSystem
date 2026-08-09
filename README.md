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
* **Optimistic UI:** Interface atualizada instantaneamente.
* **Store and Forward:** Dados gravados localmente (Room) e sincronizados em background (WorkManager) quando a conexão 4G/Wi-Fi é restabelecida, renovando automaticamente o token JWT.