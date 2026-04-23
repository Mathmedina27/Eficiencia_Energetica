# Análisis de Latencia y Energía en Lectura de Archivos Binarios

**Universidad Nacional de Loja**  
Facultad de la Energía, las Industrias y los Recursos Naturales No Renovables  
Carrera de Computación — Estructura de Datos  
**Docente:** Ing. Cristian Narváez G. Mg.Sc. | **Período:** Abril – Agosto 2026  
**Grupo H:** Joseph Aguilar · Mathias Medina · Gerardo Naula · Cristian Correa

---

## Descripción

Benchmark en Java que compara tres estrategias de lectura de archivos binarios de 1 GB, midiendo **latencia** (tiempo de respuesta) y **consumo energético estimado**. El caso de uso está orientado a plataformas de alto tráfico como portales de noticias regionales que necesitan ingestar logs y archivos multimedia antes de almacenarlos en bases de datos como Supabase.

---

## Métodos Comparados

| Método | Mecanismo | Copias de datos |
|---|---|---|
| **Stream IO** | `FileInputStream` + `DataInputStream` + buffer 8KB | 2 (kernel → heap Java) |
| **NIO Heap Buffer** | `FileChannel` + `ByteBuffer.allocate(8192)` | 2 (kernel → heap Java) |
| **Memory Mapped** | `FileChannel.map()` + `MappedByteBuffer` (Zero-Copy) | 0 (acceso directo al page cache) |

---

## Resultados Experimentales

### Latencia

| Método | Promedio | Desviación estándar |
|---|---|---|
| Stream IO | 1528.42 ms | 14.96 ms |
| NIO Heap Buffer | 954.03 ms | 10.16 ms |
| Memory Mapped | 546.05 ms | 15.68 ms |

### Energía estimada (TDP = 105W, Ryzen 7 alto rendimiento)

| Método | Energía estimada | Eficiencia |
|---|---|---|
| Stream IO | 50.68 J | 1.06 × 10⁸ Bytes/J |
| NIO Heap Buffer | 31.55 J | 1.70 × 10⁸ Bytes/J |
| Memory Mapped | 18.52 J | 2.90 × 10⁸ Bytes/J |

> **Memory Mapped redujo el consumo energético un 63% respecto a Stream IO.**

---

## Estructura del Proyecto

```
├── Main.java          # Código fuente principal
└── README.md
```

`Main.java` contiene:
- `generarArchivo()` — genera el archivo binario de 1 GB con registros de `ID (long) + Timestamp (long) + Contenido (byte[1024])`
- `leerStream()` — lectura con I/O tradicional bloqueante
- `leerNIO()` — lectura con FileChannel y HeapByteBuffer
- `leerMapped()` — lectura con MappedByteBuffer (Zero-Copy)
- `medir()` — ejecuta 5 iteraciones con warm-up previo, calcula media, desviación estándar, energía y eficiencia

---

## Cómo ejecutar

**Requisitos:** Java 17+

```bash
javac Main.java
java Main
```

El programa genera automáticamente `archivo_datos.bin` (~1 GB) si no existe, realiza 3 iteraciones de warm-up y luego ejecuta la medición oficial con 5 iteraciones por método.

> **Nota sobre medición energética:** Se usó estimación basada en TDP del CPU como alternativa a jRAPL, que solo es compatible con procesadores Intel y no con AMD Ryzen.

---

## Conclusiones

- **Memory Mapped IO** es el método más rápido y eficiente energéticamente para archivos grandes, gracias al principio de Zero-Copy y la eliminación de context switches.
- **NIO Heap Buffer** ofrece una mejora del ~38% en velocidad respecto a Stream IO al reducir el número de syscalls.
- **Stream IO** presenta la mayor inestabilidad (mayor desviación estándar) por el overhead de múltiples llamadas al sistema operativo.
- Memory Mapped IO **no es recomendable para archivos pequeños**, donde el costo de mapear puede superar el beneficio obtenido.
