package sistema.arquivos;

import sistema.objetos.Fechamento;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Carrega e salva fechamentos em ~/Documents/fechamentos_db.txt
 * Também gera arquivo individual por fechamento com data no nome.
 */
public class FechamentoRepository {

    private static final String SAVE_PATH = System.getProperty("user.home")
            + System.getProperty("file.separator") + "Documents"
            + System.getProperty("file.separator") + "fechamentos_db.txt";

    public static List<Fechamento> loadAll() {
        List<Fechamento> out = new ArrayList<>();
        Path path = Path.of(SAVE_PATH);

        if (!Files.exists(path)) return out;

        try {
            List<String> lines = Files.readAllLines(path);
            List<String> block = new ArrayList<>();

            for (String ln : lines) {
                if (ln == null) continue;
                ln = ln.trim();
                if (ln.isEmpty()) continue;
                block.add(ln);

                if ("END".equals(ln)) {
                    try {
                        Fechamento f = Fechamento.fromBlockLines(block);
                        out.add(f);
                    } catch (Exception e) {
                        System.err.println("Erro ao carregar bloco: " + e.getMessage());
                    }
                    block.clear();
                }
            }
        } catch (IOException e) {
            System.err.println("Erro ao ler persistência: " + e.getMessage());
        }
        return out;
    }

    public static void saveAll(List<Fechamento> lista) {
        Path path = Path.of(SAVE_PATH);

        try {
            Files.createDirectories(path.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(
                    path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                for (Fechamento f : lista) {
                    for (String ln : f.toBlockLines()) {
                        writer.write(ln);
                        writer.newLine();
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("Erro ao salvar persistência: " + e.getMessage());
        }
    }

    public static void saveIndividualFile(Fechamento f) {
        String fname = String.format("fechamento_%02d-%02d-%04d_turno%d.txt",
                f.getData().getDayOfMonth(),
                f.getData().getMonthValue(),
                f.getData().getYear(),
                f.getTurno());

        Path p = Path.of(System.getProperty("user.home"), "Documents", fname);

        try {
            Files.createDirectories(p.getParent());

            try (BufferedWriter w = Files.newBufferedWriter(
                    p, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                for (String ln : f.toBlockLines()) {
                    w.write(ln);
                    w.newLine();
                }
            }

        } catch (IOException e) {
            System.err.println("Erro ao gerar arquivo individual: " + e.getMessage());
        }
    }

    public static String getSavePath() {
        return SAVE_PATH;
    }
}
