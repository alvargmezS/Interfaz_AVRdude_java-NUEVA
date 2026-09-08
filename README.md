# AVRdude Interface

Interfaz gráfica multiplataforma para **AVRdude** escrita en Java. Permite flashear microcontroladores AVR, leer/escribir fuses y gestionar programadores desde una interfaz moderna.

![Java](https://img.shields.io/badge/Java-8%2B-blue?logo=openjdk)
![Platform](https://img.shields.io/badge/macOS-soporte-green?logo=apple)
![Platform](https://img.shields.io/badge/Linux-soporte-green?logo=linux)
![Platform](https://img.shields.io/badge/Windows-no_disponible-red?logo=windows)

---

## Funcionalidades

- **Write/Read .hex** — Escritura y lectura de firmware en microcontroladores AVR
- **Verificar .hex** — Verificación de flash contra archivo hexadecimal
- **Read/Write fuses** — Lectura y escritura de fuses (low, high, extended) con soporte para valores hexadecimales y binarios
- **Lista de microcontroladores y programadores** — Listado formateado en tabla con IDs y descripciones
- **Prueba de conexión** — Verificación real de la cadena USB→ISP→micro: lee la firma del chip y muestra un veredicto explícito de éxito o error
- **Velocidad SCK fija (bitclock `-B`)** — Fija la velocidad del reloj ISP para que ni avrdude ni el firmware del programador la ajusten automáticamente por software. Por defecto 187.5 kHz (seguro para objetivos a ≥ 750 kHz, incluidos chips vírgenes a 1 MHz); seleccionable entre 3 MHz y 8 kHz, o Auto (comportamiento de avrdude)
- **Consola con colores** — Salida coloreada por tipo: errores (rojo), warnings (amarillo), fuses (cian), éxito (verde), progreso (gris)
- **Elevación de privilegios automática** — `sudo` en macOS/Linux, `UAC` en Windows
- **Interfaz moderna** — FlatLaf con fallback al Look & Feel del sistema

---

## Plataformas soportadas

| Plataforma | Estado | Elevación de privilegios | Notas |
|------------|--------|--------------------------|-------|
| **macOS** | ✅ Funcional | `sudo -S` con JPasswordField + `--preserve-env=PATH` | Detección automática de Homebrew (`/opt/homebrew/bin`) |
| **Linux** | ✅ Funcional | Cascada: `pkexec` → `gksudo` → `kdesudo` → `sudo -S` | Soporte para X11 y Wayland |
| **Windows** | ❌ No disponible | UAC vía VBS | La elevación vía VBS no ha sido probada. El resto de la aplicación no está adaptada para Windows |

---

## Requisitos

- **Java 8** o superior
- **AVRdude** instalado y accesible en el PATH
  - macOS: `brew install avrdude`
  - Linux: `sudo apt install avrdude`
- FlatLaf (opcional, incluido en `lib/`)

## Compilación

```bash
javac -cp "lib/flatlaf-3.7.1.jar:src" -d out $(find src -name "*.java")
```

## Ejecución

```bash
java -cp "out:lib/flatlaf-3.7.1.jar" interfaz.interfaz
```

## Crear JAR ejecutable

```bash
jar cfe Interfaz_AVRdude.jar interfiz.interfaz -C out .
java -jar Interfaz_AVRdude.jar
```

> Si FlatLaf no está en el classpath, la aplicación usa el Look & Feel del sistema sin errores.

---

## Estructura del proyecto

```
src/
├── interfaz/
│   ├── interfaz.java          # Entry point: LAF cascade, elevación, EDT
│   └── Elevator.java          # Elevación de privilegios (sudo/pkexec/UAC)
├── GUI/
│   ├── frame.java              # JFrame con menú e icono
│   ├── panel.java              # UI principal: consola, toolbar, fuses, status
│   └── conf_pantalla.java     # Utilidad de tamaño de pantalla
└── control_avrdudes/
    └── control_avrdudes.java  # ProcessBuilder, PATH injection, parsing
lib/
└── flatlaf-3.7.1.jar          # FlatLaf (dependencia opcional)
```

---

## Novedades respecto a la versión original

- **Interfaz completamente renovada** — FlatLaf, paleta de 16+ colores semánticos, botones flat con hover, status bar centrada con chips
- **Consola con colores** — JTextPane + StyledDocument con 10 estilos, timestamps, encabezados Unicode, tablas formateadas
- **Elevación de privilegios automática** — No requiere ejecutar `sudo java -jar ...` manualmente; la aplicación solicita la contraseña
- **Preservación de PATH** — Triple protección para que `avrdude` sea encontrable tras la elevación con `sudo` (`--preserve-env`, inyección en ProcessBuilder, inyección en control_avrdudes)
- **Threading correcto** — SwingWorker en vez de bloquear el EDT con `Runtime.exec()`
- **Procesamiento robusto** — ProcessBuilder con UTF-8, timeout de 30s, detección de errores específica
- **Soporte macOS** — `apple.laf.useScreenMenuBar`, SF Pro Text, detección de Homebrew
- **15+ bugs corregidos** — `==` vs `.equals()`, `|` vs `||`, `showOpenDialog` vs `showSaveDialog`, conversión binaria, charset, `return` en `finally`, etc.

---

## Bugs conocidos

- La escritura de fuses puede fallar en microcontroladores con menos de 3 fuses
- El soporte para Windows no está funcional (la elevación UAC vía VBS no ha sido probada)

---

## Licencia

LICENCIA DE USO NO COMERCIAL