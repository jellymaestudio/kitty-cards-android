# Kitty Cards Android

A simplified Android recreation of the **Kitty Cards** mini-game from *Love and Deepspace*.

The app allows two players to play against each other locally using Bluetooth Low Energy.

## Features

- Two-player matches via Bluetooth Low Energy
- Host and join lobby
- Guest discovery and selection
- Synchronized game state on both devices
- Three-round matches
- Randomized cards, board colors and starting player
- Automatic score calculation
- Match results and disconnect handling

## Game Rules

Kitty Cards is played on a 3 × 3 board. The center field is used to draw cards.

Each card has:

- one of four colors
- a value between 1 and 6

Players alternate between drawing or playing one card.

The score of a played card depends on the field:

- Grey field: normal card value
- Matching color: double card value
- Different color: no points

A round ends when the board is full. A match consists of up to three rounds and ends once one player has won two rounds or all three rounds have been completed.

## Architecture

The application is divided into four main components:

- **GUI** – displays the application state and forwards user interactions
- **GameController** – manages game logic, validation and match synchronization
- **NetworkManager** – manages Bluetooth connections and data transfer
- **ProtocolEngine** – encodes and decodes network messages

The components communicate through the interfaces:

- `IGameController`
- `INetworkManager`
- `IProtocolEngine`

The host and join screens use separate lobby controllers to keep UI code and application logic separated.

Dependency injection is implemented with **Hilt**.

## Technologies

- Android
- Kotlin
- Java
- Bluetooth Low Energy
- Hilt
- Gradle
- JUnit
- Mockito
- Espresso

## Project Structure

```text
app/src/main/java/kittycards/kittycardsandroid/
├── components/   Interfaces between the main components
├── di/           Hilt dependency injection configuration
├── logic/        Game and lobby controllers
├── model/        Cards, players, board and match state
├── network/      BLE communication and network protocol
└── ui/           Android activities and UI utilities
