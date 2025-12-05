package main.java.sistema;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;

import main.java.sistema.arquivos.FechamentoRepository;
import main.java.sistema.objetos.Fechamento;
import main.java.sistema.objetos.MaquinaCartao;

/**
 * Main completo do sistema + resultado final do turno integrado.
 */
public class Principal {

    private static final Scanner scan = new Scanner(System.in);
    private static List<Fechamento> lista;

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter INPUT_FMT = DateTimeFormatter.ofPattern("d/M/yyyy");

    private static final double TAXA_FACTOR = 0.97;

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

    // ----------------------
    // CRIAR FECHAMENTO
    // ----------------------
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

            cred *= TAXA_FACTOR;
            deb *= TAXA_FACTOR;
            pix *= TAXA_FACTOR;

            System.out.printf(
                    "Valores após taxa: Crédito=R$ %.2f | Débito=R$ %.2f | Pix=R$ %.2f%n",
                    cred, deb, pix
            );

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

        // Ajuste turno 2
        if (turno == 2) {
            Fechamento turno1 = null;
            for (Fechamento fx : lista) {
                if (fx.getData() != null && fx.getData().equals(data) && fx.getTurno() == 1) {
                    turno1 = fx;
                    break;
                }
            }

            if (turno1 != null) {
                System.out.println("\n>> Turno 1 encontrado. Aplicando diferença...");

                double difCred = f.totalCreditoMaquinas() - turno1.totalCreditoMaquinas();
                double difDeb = f.totalDebitoMaquinas() - turno1.totalDebitoMaquinas();
                double difPix = f.totalPixMaquinas() - turno1.totalPixMaquinas();

                f.getMaquinas().clear();
                f.addMaquina(new MaquinaCartao(difCred, difDeb, difPix));

                System.out.printf("Máquina diferença -> Cred: %.2f | Deb: %.2f | Pix: %.2f%n",
                        difCred, difDeb, difPix);
            }
        }

        lista.add(f);
        FechamentoRepository.saveAll(lista);
        FechamentoRepository.saveIndividualFile(f);

        exibirDiferencasCurta(f);

        // 🔥 NOVO: exibir resultado total final
        System.out.printf("RESULTADO FINAL DO TURNO: R$ %.2f%n", f.getResultadoFinalTurno());

        System.out.println("Fechamento criado e salvo.");
    }

    // ----------------------
    // LISTAR / DETALHE
    // ----------------------
    private static void listar() {
        if (lista.isEmpty()) {
            System.out.println("Nenhum fechamento salvo.");
            return;
        }

        System.out.println("\n--- Fechamentos ---");
        for (int i = 0; i < lista.size(); i++) {
            Fechamento f = lista.get(i);
            System.out.printf("[%d] %s - %s - Turno: %s%n",
                    i,
                    f.getData() == null ? "" : f.getData().format(DISPLAY_FMT),
                    f.getResponsavel(),
                    f.getTurno() == 1 ? "Manhã" : "Tarde/Noite"
            );
        }
    }

    private static void exibirDetalhe() {
        if (lista.isEmpty()) {
            System.out.println("Nenhum fechamento salvo.");
            return;
        }

        int idx = lerIntMinMax("Índice do fechamento para exibir: ", 0, lista.size() - 1);
        Fechamento f = lista.get(idx);

        System.out.println(f);

        exibirDiferencasDetalhado(f);

        // 🔥 NOVO
        System.out.printf("\n>>> RESULTADO FINAL DO TURNO: R$ %.2f%n", f.getResultadoFinalTurno());
    }

    // ----------------------
    // EDITAR / EXCLUIR
    // ----------------------
    private static void editar() {
        if (lista.isEmpty()) {
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

        int novoTurno = lerTurnoAllowSkip("Novo turno [1/2 | enter=manter]: ");
        if (novoTurno != -1) f.setTurno(novoTurno);

        // editar máquinas
        System.out.println("Deseja (1) substituir máquinas, (2) editar individual, (0) pular?");
        int op = lerInt("Opção: ");

        if (op == 1) {
            f.clearMaquinas();
            int qtd = lerIntMin("Quantas máquinas? ", 0);

            for (int i = 1; i <= qtd; i++) {
                System.out.println("-- Máquina " + i + " --");

                double cred = lerDoubleNonNegative("Crédito (R$): ") * TAXA_FACTOR;
                double deb = lerDoubleNonNegative("Débito (R$): ") * TAXA_FACTOR;
                double pix = lerDoubleNonNegative("Pix (R$): ") * TAXA_FACTOR;

                f.addMaquina(new MaquinaCartao(cred, deb, pix));
            }

        } else if (op == 2) {
            if (f.getMaquinas().isEmpty()) {
                System.out.println("Não há máquinas.");
            } else {
                for (int i = 0; i < f.getMaquinas().size(); i++) {
                    MaquinaCartao m = f.getMaquinas().get(i);

                    System.out.println("Máquina " + (i + 1) + ": " + m);

                    if (lerTextoAllowSkip("Editar? (s/enter): ").equalsIgnoreCase("s")) {
                        double cred = lerDoubleAllowSkip("Crédito [enter mantém]: ", m.getCredito()) * TAXA_FACTOR;
                        double deb = lerDoubleAllowSkip("Débito [enter mantém]: ", m.getDebito()) * TAXA_FACTOR;
                        double pix = lerDoubleAllowSkip("Pix [enter mantém]: ", m.getPix()) * TAXA_FACTOR;

                        m.setCredito(cred);
                        m.setDebito(deb);
                        m.setPix(pix);
                    }
                }
            }
        }

        // relatórios e dinheiro
        f.setRelatorioCredito(lerDoubleAllowSkip("Relatório Crédito [enter mantém]: ", f.getRelatorioCredito()));
        f.setRelatorioDebito(lerDoubleAllowSkip("Relatório Débito [enter mantém]: ", f.getRelatorioDebito()));
        f.setRelatorioPix(lerDoubleAllowSkip("Relatório Pix [enter mantém]: ", f.getRelatorioPix()));
        f.setEntradaDinheiro(lerDoubleAllowSkip("Dinheiro contado [enter mantém]: ", f.getEntradaDinheiro()));
        f.setRelatorioDinheiro(lerDoubleAllowSkip("Relatório Dinheiro [enter mantém]: ", f.getRelatorioDinheiro()));
        f.setTrocoInicial(lerDoubleAllowSkip("Troco inicial [enter mantém]: ", f.getTrocoInicial()));

        FechamentoRepository.saveAll(lista);
        FechamentoRepository.saveIndividualFile(f);

        System.out.println("Fechamento atualizado e salvo.");
    }

    private static void excluir() {
        if (lista.isEmpty()) {
            System.out.println("Nenhum fechamento salvo.");
            return;
        }

        int idx = lerIntMinMax("Índice para excluir: ", 0, lista.size() - 1);
        Fechamento f = lista.remove(idx);

        FechamentoRepository.saveAll(lista);

        System.out.println("Fechamento removido: "
                + f.getResponsavel() + " - "
                + (f.getData() == null ? "" : f.getData().format(DISPLAY_FMT)));
    }

    // ----------------------
    // DIFERENÇAS
    // ----------------------
    private static void exibirDiferencasCurta(Fechamento f) {
        System.out.println("\n=== DIFERENÇAS ===");
        System.out.printf("CRÉDITO : R$ %.2f%n", f.getDiferencaCredito());
        System.out.printf("DÉBITO  : R$ %.2f%n", f.getDiferencaDebito());
        System.out.printf("PIX     : R$ %.2f%n", f.getDiferencaPix());
        System.out.printf("DINHEIRO: R$ %.2f%n", f.getDiferencaDinheiro());
        System.out.println("-------------------------");
    }

    private static void exibirDiferencasDetalhado(Fechamento f) {
        System.out.println("\n=== DIFERENÇAS DETALHADAS ===");

        System.out.printf("Crédito - Rel: R$ %.2f | Máquinas: R$ %.2f | Dif: R$ %.2f%n",
                f.getRelatorioCredito(), f.totalCreditoMaquinas(), f.getDiferencaCredito());

        System.out.printf("Débito  - Rel: R$ %.2f | Máquinas: R$ %.2f | Dif: R$ %.2f%n",
                f.getRelatorioDebito(), f.totalDebitoMaquinas(), f.getDiferencaDebito());

        System.out.printf("Pix     - Rel: R$ %.2f | Máquinas: R$ %.2f | Dif: R$ %.2f%n",
                f.getRelatorioPix(), f.totalPixMaquinas(), f.getDiferencaPix());

        System.out.printf("Dinheiro- Rel: R$ %.2f | Contado : R$ %.2f | Dif: R$ %.2f%n",
                f.getRelatorioDinheiro(), f.getEntradaDinheiro(), f.getDiferencaDinheiro());

        System.out.println("--------------------------------");
    }

    // ----------------------
    // LEITURA
    // ----------------------
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
            String s = scan.nextLine().trim();
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
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
            System.out.printf("Digite entre %d e %d%n", min, max);
        }
    }

    private static double lerDoubleNonNegative(String msg) {
        while (true) {
            System.out.print(msg);
            String s = scan.nextLine().trim().replace(",", ".");
            try {
                double v = Double.parseDouble(s);
                if (v < 0) throw new Exception();
                return v;
            } catch (Exception e) {
                System.out.println("Valor inválido.");
            }
        }
    }

    private static double lerDoubleAllowSkip(String msg, double atual) {
        System.out.print(msg);
        String s = scan.nextLine().trim();
        if (s.isBlank()) return atual;

        try {
            return Double.parseDouble(s.replace(",", "."));
        } catch (Exception e) {
            System.out.println("Entrada inválida. Mantendo.");
            return atual;
        }
    }

    private static LocalDate lerDataAllowToday(String msg) {
        System.out.print(msg);
        String s = scan.nextLine().trim();
        if (s.isBlank()) return LocalDate.now();

        try {
            return LocalDate.parse(s, INPUT_FMT);
        } catch (Exception e) {
            System.out.println("Formato inválido. Usando hoje.");
            return LocalDate.now();
        }
    }

    private static LocalDate lerDataAllowSkip(String msg) {
        System.out.print(msg);
        String s = scan.nextLine().trim();
        if (s.isBlank()) return null;

        try {
            return LocalDate.parse(s, INPUT_FMT);
        } catch (Exception e) {
            System.out.println("Formato inválido. Mantendo.");
            return null;
        }
    }

    private static int lerOpcaoTurno() {
        while (true) {
            System.out.print("Turno (1=Manhã | 2=Tarde/Noite): ");
            String s = scan.nextLine();
            try {
                int t = Integer.parseInt(s);
                if (t == 1 || t == 2) return t;
            } catch (Exception ignore) {}
            System.out.println("Digite 1 ou 2.");
        }
    }

    private static int lerTurnoAllowSkip(String msg) {
        System.out.print(msg);
        String s = scan.nextLine();
        if (s.isBlank()) return -1;

        try {
            int t = Integer.parseInt(s);
            if (t == 1 || t == 2) return t;
        } catch (Exception ignore) {}

        System.out.println("Entrada inválida.");
        return -1;
    }
}
