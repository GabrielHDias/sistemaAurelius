package sistema;

import sistema.arquivos.FechamentoRepository;
import sistema.objetos.Fechamento;
import sistema.objetos.MaquinaCartao;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

/**
 * Main completo do sistema.
 * - Cria / Lista / Exibe / Edita / Exclui fechamentos
 * - Persiste via FechamentoRepository
 * - Aplica taxa de 3% sobre valores das máquinas (crédito, débito, pix)
 * - Ao criar turno 2, se existir turno 1 do mesmo dia, substitui máquinas do turno 2
 *   por UMA máquina representando a diferença (turno2 - turno1).
 */
public class Principal {

    private static final Scanner scan = new Scanner(System.in);
    private static List<Fechamento> lista;
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("d/M/yyyy");

    private static final double TAXA_FACTOR = 0.97; // aplica 3%

    public static void main(String[] args) {
        lista = FechamentoRepository.loadAll();
        System.out.println("Arquivo de persistência: " + FechamentoRepository.getSavePath());
        abrirMenu();
    }

    private static void abrirMenu() {
        while (true) {
            System.out.println("\n==== MENU ====");
            System.out.println("1 - Novo fechamento");
            System.out.println("2 - Listar fechamentos");
            System.out.println("3 - Exibir fechamento (detalhe)");
            System.out.println("4 - Editar fechamento");
            System.out.println("5 - Excluir fechamento");
            System.out.println("0 - Salvar e sair");
            int opt = lerInt("Escolha: ");
            switch (opt) {
                case 1 -> novoFechamento();
                case 2 -> listar();
                case 3 -> exibirDetalhe();
                case 4 -> editar();
                case 5 -> excluir();
                case 0 -> {
                    FechamentoRepository.saveAll(lista);
                    System.out.println("Salvo em: " + FechamentoRepository.getSavePath());
                    System.out.println("Tchau!");
                    return;
                }
                default -> System.out.println("Opção inválida.");
            }
        }
    }

    // -------------------------
    // CRIAR FECHAMENTO
    // -------------------------
    private static void novoFechamento() {
        System.out.println("\n--- Novo Fechamento ---");

        String resp = lerTexto("Responsável: ");
        LocalDate data = lerDataAllowToday("Data (Dia/Mês/Ano) [enter = hoje]: ");
        int turno = lerOpcaoTurno();

        Fechamento f = new Fechamento(resp, data, turno);

        int qtd = lerIntMin("Quantas máquinas de cartão foram usadas? ", 0);
        for (int i = 1; i <= qtd; i++) {
            System.out.println("-- Máquina " + i + " --");
            double cred = lerDoubleNonNegative("Crédito (R$): ");
            double deb = lerDoubleNonNegative("Débito (R$): ");
            double pix = lerDoubleNonNegative("Pix (R$): ");

            // Aplicar taxa de 3% (opção A: taxa sobre o total é equivalente a aplicar 3% em cada campo)
            cred *= TAXA_FACTOR;
            deb *= TAXA_FACTOR;
            pix *= TAXA_FACTOR;

            System.out.printf("Valores após taxa (3%%): Crédito=R$ %.2f | Débito=R$ %.2f | Pix=R$ %.2f%n",
                    cred, deb, pix);

            f.addMaquina(new MaquinaCartao(cred, deb, pix));
        }

        System.out.println("\n--- Relatórios informados (operadora/cofre) ---");
        f.setRelatorioCredito(lerDoubleNonNegative("Relatório Crédito (R$): "));
        f.setRelatorioDebito(lerDoubleNonNegative("Relatório Débito (R$): "));
        f.setRelatorioPix(lerDoubleNonNegative("Relatório Pix (R$): "));

        System.out.println("\n--- Dinheiro ---");
        f.setEntradaDinheiro(lerDoubleNonNegative("Entrada contada em dinheiro (R$): "));
        f.setRelatorioDinheiro(lerDoubleNonNegative("Relatório Dinheiro (R$): "));
        f.setTrocoInicial(lerDoubleNonNegative("Troco inicial (R$): "));

        // --- Ajuste automático de diferença entre turnos (somente máquinas) ---
        if (turno == 2) {
            // Buscar fechamento do mesmo dia, turno 1
            Fechamento turno1 = null;
            for (Fechamento fx : lista) {
                if (fx.getData() != null && fx.getData().equals(data) && fx.getTurno() == 1) {
                    turno1 = fx;
                    break;
                }
            }

            if (turno1 != null) {
                System.out.println("\n>> Detectado fechamento do TURNO 1 no mesmo dia.");
                System.out.println(">> Aplicando diferença: Turno2 - Turno1 (somando todas as máquinas).");

                // Somatório do turno 1 (já gravado com taxa aplicada no momento em que foi criado)
                double t1Cred = turno1.totalCreditoMaquinas();
                double t1Deb = turno1.totalDebitoMaquinas();
                double t1Pix = turno1.totalPixMaquinas();

                // Somatório do turno 2 (o fechamento atual f contém máquinas já com taxa)
                double t2Cred = f.totalCreditoMaquinas();
                double t2Deb = f.totalDebitoMaquinas();
                double t2Pix = f.totalPixMaquinas();

                // Diferença final (turno2 - turno1)
                double difCred = t2Cred - t1Cred;
                double difDeb = t2Deb - t1Deb;
                double difPix = t2Pix - t1Pix;

                // Substitui todas as máquinas do turno 2 por uma única máquina diferença
                f.getMaquinas().clear();
                f.addMaquina(new MaquinaCartao(difCred, difDeb, difPix));

                System.out.println(">> Máquina de diferença aplicada:");
                System.out.printf("   Crédito: %.2f | Débito: %.2f | Pix: %.2f%n",
                        difCred, difDeb, difPix);
            }
        }

        // Salva
        lista.add(f);
        FechamentoRepository.saveAll(lista);
        FechamentoRepository.saveIndividualFile(f);

        exibirDiferencasCurta(f);
        System.out.println("Fechamento criado e salvo.");
    }

    // -------------------------
    // LISTAR / EXIBIR
    // -------------------------
    private static void listar() {
        if (lista == null || lista.isEmpty()) {
            System.out.println("Nenhum fechamento salvo.");
            return;
        }
        System.out.println("\n--- Fechamentos ---");
        for (int i = 0; i < lista.size(); i++) {
            Fechamento f = lista.get(i);
            String dataStr = f.getData() == null ? "" : f.getData().format(DISPLAY_FMT);
            System.out.printf("[%d] %s - %s - Turno: %s%n", i,
                    dataStr,
                    f.getResponsavel(),
                    f.getTurno() == 1 ? "Manhã" : "Tarde/Noite");
        }
    }

    private static void exibirDetalhe() {
        if (lista == null || lista.isEmpty()) {
            System.out.println("Nenhum fechamento salvo.");
            return;
        }
        int idx = lerIntMinMax("Índice do fechamento para exibir: ", 0, lista.size() - 1);
        Fechamento f = lista.get(idx);
        System.out.println(f.toString());
        exibirDiferencasDetalhado(f);
    }

    // -------------------------
    // EDITAR
    // -------------------------
    private static void editar() {
        if (lista == null || lista.isEmpty()) {
            System.out.println("Nenhum fechamento salvo.");
            return;
        }

        int idx = lerIntMinMax("Índice do fechamento para editar: ", 0, lista.size() - 1);
        Fechamento f = lista.get(idx);

        System.out.println("\nEditando fechamento (enter para manter):");
        System.out.println(f);

        String novoResp = lerTextoAllowSkip("Novo responsável [enter para manter]: ");
        if (!novoResp.isBlank()) f.setResponsavel(novoResp);

        LocalDate novaData = lerDataAllowSkip("Nova data (Dia/Mês/Ano) [enter para manter]: ");
        if (novaData != null) f.setData(novaData);

        int novoTurno = lerTurnoAllowSkip("Novo turno [1=Manhã | 2=Tarde/Noite | enter=manter]: ");
        if (novoTurno != -1) f.setTurno(novoTurno);

        // Opção de editar máquinas (substituir / editar individual)
        System.out.println("Deseja (1) substituir máquinas, (2) editar individual, (0) pular?");
        int op = lerInt("Opção: ");
        if (op == 1) {
            f.clearMaquinas();
            int qtd = lerIntMin("Quantas máquinas (nova lista)?: ", 0);
            for (int i = 1; i <= qtd; i++) {
                System.out.println("-- Máquina " + i + " --");
                double cred = lerDoubleNonNegative("Crédito (R$): ");
                double deb = lerDoubleNonNegative("Débito (R$): ");
                double pix = lerDoubleNonNegative("Pix (R$): ");

                // Aplicar taxa também ao editar/reatribuir
                cred *= TAXA_FACTOR;
                deb *= TAXA_FACTOR;
                pix *= TAXA_FACTOR;

                f.addMaquina(new MaquinaCartao(cred, deb, pix));
            }
        } else if (op == 2) {
            if (f.getMaquinas().isEmpty()) {
                System.out.println("Não há máquinas a editar.");
            } else {
                for (int i = 0; i < f.getMaquinas().size(); i++) {
                    MaquinaCartao m = f.getMaquinas().get(i);
                    System.out.println("Máquina " + (i + 1) + ": " + m);
                    String s = lerTextoAllowSkip("Editar esta? (s para sim, enter para pular): ");
                    if (s.equalsIgnoreCase("s")) {
                        double cred = lerDoubleAllowSkip("Crédito [enter para manter]: ", m.getCredito());
                        double deb = lerDoubleAllowSkip("Débito [enter para manter]: ", m.getDebito());
                        double pix = lerDoubleAllowSkip("Pix [enter para manter]: ", m.getPix());

                        // Ao editar, re-aplica taxa (assume-se que usuário informou valores brutos)
                        cred *= TAXA_FACTOR;
                        deb *= TAXA_FACTOR;
                        pix *= TAXA_FACTOR;

                        m.setCredito(cred);
                        m.setDebito(deb);
                        m.setPix(pix);
                    }
                }
            }
        }

        // Editar relatórios / dinheiro
        double rc = lerDoubleAllowSkip("Relatório Crédito [enter para manter]: ", f.getRelatorioCredito());
        double rd = lerDoubleAllowSkip("Relatório Débito [enter para manter]: ", f.getRelatorioDebito());
        double rp = lerDoubleAllowSkip("Relatório Pix [enter para manter]: ", f.getRelatorioPix());
        double din = lerDoubleAllowSkip("Entrada contada dinheiro [enter para manter]: ", f.getEntradaDinheiro());
        double rdin = lerDoubleAllowSkip("Relatório Dinheiro [enter para manter]: ", f.getRelatorioDinheiro());
        double tro = lerDoubleAllowSkip("Troco inicial [enter para manter]: ", f.getTrocoInicial());

        f.setRelatorioCredito(rc);
        f.setRelatorioDebito(rd);
        f.setRelatorioPix(rp);
        f.setEntradaDinheiro(din);
        f.setRelatorioDinheiro(rdin);
        f.setTrocoInicial(tro);

        FechamentoRepository.saveAll(lista);
        FechamentoRepository.saveIndividualFile(f);
        System.out.println("Fechamento atualizado e salvo.");
    }

    // -------------------------
    // EXCLUIR
    // -------------------------
    private static void excluir() {
        if (lista == null || lista.isEmpty()) {
            System.out.println("Nenhum fechamento salvo.");
            return;
        }
        int idx = lerIntMinMax("Índice do fechamento para excluir: ", 0, lista.size() - 1);
        Fechamento removed = lista.remove(idx);
        FechamentoRepository.saveAll(lista);
        System.out.println("Fechamento removido: " + removed.getResponsavel() + " - " +
                (removed.getData() == null ? "" : removed.getData().format(DISPLAY_FMT)));
    }

    // -------------------------
    // DIFERENÇAS (curto / detalhado)
    // -------------------------
    private static void exibirDiferencasCurta(Fechamento f) {
        System.out.println("\n=== DIFERENÇAS ===");
        System.out.printf("CRÉDITO: Dif = R$ %.2f%n", f.getDiferencaCredito());
        System.out.printf("DÉBITO : Dif = R$ %.2f%n", f.getDiferencaDebito());
        System.out.printf("PIX    : Dif = R$ %.2f%n", f.getDiferencaPix());
        System.out.printf("DINHEIRO: Dif = R$ %.2f%n", f.getDiferencaDinheiro());
        System.out.println("-------------------------");
    }

    private static void exibirDiferencasDetalhado(Fechamento f) {
        System.out.println("\n=== DIFERENÇAS DETALHADAS ===");
        System.out.printf("Crédito - Rel: R$ %.2f | Máquinas (líq): R$ %.2f | Dif: R$ %.2f%n",
                f.getRelatorioCredito(), f.totalCreditoMaquinas() * (1.0), f.getDiferencaCredito());
        System.out.printf("Débito  - Rel: R$ %.2f | Máquinas (líq): R$ %.2f | Dif: R$ %.2f%n",
                f.getRelatorioDebito(), f.totalDebitoMaquinas() * (1.0), f.getDiferencaDebito());
        System.out.printf("Pix     - Rel: R$ %.2f | Máquinas : R$ %.2f | Dif: R$ %.2f%n",
                f.getRelatorioPix(), f.totalPixMaquinas(), f.getDiferencaPix());
        System.out.printf("Dinheiro- Rel: R$ %.2f | Contado : R$ %.2f | Dif: R$ %.2f%n",
                f.getRelatorioDinheiro(), f.getEntradaDinheiro(), f.getDiferencaDinheiro());
    }

    // -------------------------
    // MÉTODOS DE LEITURA / UTILITÁRIOS
    // -------------------------
    private static String lerTexto(String msg) {
        System.out.print(msg);
        return scan.nextLine().trim();
    }

    private static String lerTextoAllowSkip(String msg) {
        System.out.print(msg);
        return scan.nextLine();
    }

    private static int lerInt(String msg) {
        while (true) {
            System.out.print(msg);
            String line = scan.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Digite um inteiro válido.");
            }
        }
    }

    private static int lerIntMin(String msg, int min) {
        while (true) {
            int v = lerInt(msg);
            if (v >= min) return v;
            System.out.println("Valor mínimo: " + min);
        }
    }

    private static int lerIntMinMax(String msg, int min, int max) {
        while (true) {
            int v = lerInt(msg);
            if (v >= min && v <= max) return v;
            System.out.printf("Digite um valor entre %d e %d%n", min, max);
        }
    }

    private static double lerDoubleNonNegative(String msg) {
        while (true) {
            System.out.print(msg);
            String line = scan.nextLine().trim().replace(",", ".");
            try {
                double v = Double.parseDouble(line);
                if (v < 0) throw new NumberFormatException();
                return v;
            } catch (NumberFormatException e) {
                System.out.println("Digite um número válido (ex: 1234.56).");
            }
        }
    }

    private static double lerDoubleAllowSkip(String msg, double atual) {
        System.out.print(msg);
        String line = scan.nextLine().trim();
        if (line.isBlank()) return atual;
        try { return Double.parseDouble(line.replace(",", ".")); }
        catch (NumberFormatException e) { System.out.println("Entrada inválida. Mantendo valor atual."); return atual; }
    }

    private static LocalDate lerDataAllowToday(String msg) {
        System.out.print(msg);
        String line = scan.nextLine().trim();
        if (line.isBlank()) return LocalDate.now();
        try { return LocalDate.parse(line, INPUT_FMT); }
        catch (DateTimeParseException e) { System.out.println("Formato inválido. Usando data de hoje."); return LocalDate.now(); }
    }

    private static LocalDate lerDataAllowSkip(String msg) {
        System.out.print(msg);
        String line = scan.nextLine().trim();
        if (line.isBlank()) return null;
        try { return LocalDate.parse(line, INPUT_FMT); }
        catch (DateTimeParseException e) { System.out.println("Formato inválido. Mantendo atual."); return null; }
    }

    private static int lerOpcaoTurno() {
        while (true) {
            System.out.print("Turno (1=Manhã | 2=Tarde/Noite): ");
            String line = scan.nextLine().trim();
            try {
                int t = Integer.parseInt(line);
                if (t == 1 || t == 2) return t;
            } catch (NumberFormatException ignored) {}
            System.out.println("Digite 1 ou 2.");
        }
    }

    private static int lerTurnoAllowSkip(String msg) {
        System.out.print(msg);
        String line = scan.nextLine().trim();
        if (line.isBlank()) return -1;
        try {
            int t = Integer.parseInt(line);
            if (t == 1 || t == 2) return t;
        } catch (NumberFormatException ignored) {}
        System.out.println("Entrada inválida. Mantendo atual.");
        return -1;
    }
}
