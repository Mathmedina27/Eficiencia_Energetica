import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class Main {

    static final String ARCHIVO = "archivo_datos.bin";
    static final long TAMANO_OBJETIVO = 1024L * 1024 * 1024; // 1GB
    static final int TAM_CONTENIDO = 1024;

    static final double TDP_CPU = 105.0; //  Ajusta a tu CPU Ryzen

    public static void main(String[] args) throws Exception {

        File f = new File(ARCHIVO);
        if (!f.exists()) {
            generarArchivo();
        }
        for (int i = 0; i < 3; i++) {
            leerStream();
            leerNIO();
            leerMapped();
        }

        System.out.println("\n=== MEDICIÓN ===\n");

        medir("Stream IO", Main::leerStream);
        medir("NIO Heap Buffer", Main::leerNIO);
        medir("Memory Mapped", Main::leerMapped);
    }










    static void generarArchivo() throws IOException {
        Random random = new Random();

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(ARCHIVO)))) {

            long bytesEscritos = 0;
            long id = 1;

            while (bytesEscritos < TAMANO_OBJETIVO) {

                long timestamp = System.currentTimeMillis();

                byte[] contenido = new byte[TAM_CONTENIDO];
                random.nextBytes(contenido);

                dos.writeLong(id);
                dos.writeLong(timestamp);
                dos.write(contenido);

                bytesEscritos += 8 + 8 + contenido.length;
                id++;
            }
        }
    }







    static void leerStream() throws IOException {
        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(ARCHIVO)))) {

            byte[] contenido = new byte[TAM_CONTENIDO];

            while (true) {
                try {
                    dis.readLong();
                    dis.readLong();
                    dis.readFully(contenido);
                } catch (EOFException e) {
                    break;
                }
            }
        }
    }

    static void leerNIO() throws IOException {
        try (FileInputStream fis = new FileInputStream(ARCHIVO);
             FileChannel channel = fis.getChannel()) {

            ByteBuffer buffer = ByteBuffer.allocate(8192);
            byte[] contenido = new byte[TAM_CONTENIDO];

            while (channel.read(buffer) > 0) {
                buffer.flip();

                while (buffer.remaining() >= (16 + TAM_CONTENIDO)) {
                    buffer.getLong();
                    buffer.getLong();
                    buffer.get(contenido);
                }

                buffer.compact();
            }
        }
    }

    static void leerMapped() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ARCHIVO, "r");
             FileChannel channel = raf.getChannel()) {

            MappedByteBuffer buffer = channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    0,
                    channel.size()
            );

            byte[] contenido = new byte[TAM_CONTENIDO];

            while (buffer.remaining() >= (16 + TAM_CONTENIDO)) {
                buffer.getLong();
                buffer.getLong();
                buffer.get(contenido);
            }
        }
    }

    // ===============================
    // MEDICIÓN COMPLETA
    // ===============================
    static void medir(String nombre, IOTest test) throws Exception {

        int iteraciones = 5;
        long[] tiempos = new long[iteraciones];

        OperatingSystemMXBean osBean =
                (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        double cpuInicio = osBean.getProcessCpuLoad();

        long inicioTotal = System.nanoTime();

        for (int i = 0; i < iteraciones; i++) {

            long inicio = System.nanoTime();
            test.run();
            long fin = System.nanoTime();

            tiempos[i] = fin - inicio;

            System.out.println(nombre + " - Iteración " + (i + 1) +
                    ": " + tiempos[i] / 1_000_000 + " ms");
        }

        long finTotal = System.nanoTime();
        double cpuFin = osBean.getProcessCpuLoad();


        double media = Arrays.stream(tiempos).average().orElse(0.0);

        double suma = 0;
        for (long t : tiempos) {
            double diff = t - media;
            suma += diff * diff;
        }

        double desviacion = Math.sqrt(suma / iteraciones);

        double tiempoSegundos = (finTotal - inicioTotal) / 1_000_000_000.0;
        double cpuPromedio = (cpuInicio + cpuFin) / 2.0;

        double potencia = cpuPromedio * TDP_CPU;
        double energiaJ = potencia * tiempoSegundos;

        double bytesLeidos = TAMANO_OBJETIVO * iteraciones;
        double eficiencia = bytesLeidos / energiaJ;

        System.out.println(">> " + nombre);
        System.out.println("Promedio: " + media / 1_000_000 + " ms");
        System.out.println("Desviación estándar: " + desviacion / 1_000_000 + " ms");
        System.out.println("Energía (estimada): " + energiaJ + " J");
        System.out.println("Eficiencia: " + eficiencia + " Bytes/J");
        System.out.println("--------------------------------------\n");
    }

    interface IOTest {
        void run() throws Exception;
    }
}