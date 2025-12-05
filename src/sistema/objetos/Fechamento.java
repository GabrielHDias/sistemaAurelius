package sistema.objetos;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um fechamento de caixa.
 * Versão compatível: expõe métodos usados pela Principal.
 */
public class Fechamento {

    private String responsavel;
    private LocalDate data;
    private int turno; // 1 = manhã, 2 = tarde/noite

    private final List<MaquinaCartao> maquinas = new ArrayList<>();

    // valores informados pelo relatório (operadora / cofre)
    private double relatorioCredito;
    private double relatorioDebito;
    private double relatorioPix;
    private double relatorioDinheiro;

    // valores contados/entrada
    private double entradaDinheiro;
    private double trocoInicial;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final double TAXA_CARTAO = 0.03; // 3%

    public Fechamento() {}

    public Fechamento(String responsavel, LocalDate data, int turno) {
        setResponsavel(responsavel);
        setData(data);
        setTurno(turno);
    }

    // getters / setters
    public String getResponsavel() { return responsavel; }
    public void setResponsavel(String responsavel) {
        if (responsavel == null || responsavel.isBlank()) throw new IllegalArgumentException("Responsável inválido.");
        this.responsavel = responsavel.trim();
    }

    public LocalDate getData() { return data; }
    public void setData(LocalDate data) {
        if (data == null) throw new IllegalArgumentException("Data não pode ser nula.");
        this.data = data;
    }

    public int getTurno() { return turno; }
    public void setTurno(int turno) {
        if (turno != 1 && turno != 2) throw new IllegalArgumentException("Turno inválido (1 ou 2).");
        this.turno = turno;
    }

    public List<MaquinaCartao> getMaquinas() { return maquinas; }
    public void addMaquina(MaquinaCartao m) {
        if (m == null) throw new IllegalArgumentException("Máquina não pode ser nula.");
        maquinas.add(m);
    }
    public void clearMaquinas() { maquinas.clear(); }

    public double getRelatorioCredito() { return relatorioCredito; }
    public void setRelatorioCredito(double relatorioCredito) {
        validarNaoNegativo(relatorioCredito, "Relatório Crédito");
        this.relatorioCredito = relatorioCredito;
    }

    public double getRelatorioDebito() { return relatorioDebito; }
    public void setRelatorioDebito(double relatorioDebito) {
        validarNaoNegativo(relatorioDebito, "Relatório Débito");
        this.relatorioDebito = relatorioDebito;
    }

    public double getRelatorioPix() { return relatorioPix; }
    public void setRelatorioPix(double relatorioPix) {
        validarNaoNegativo(relatorioPix, "Relatório Pix");
        this.relatorioPix = relatorioPix;
    }

    public double getRelatorioDinheiro() { return relatorioDinheiro; }
    public void setRelatorioDinheiro(double relatorioDinheiro) {
        validarNaoNegativo(relatorioDinheiro, "Relatório Dinheiro");
        this.relatorioDinheiro = relatorioDinheiro;
    }

    public double getEntradaDinheiro() { return entradaDinheiro; }
    public void setEntradaDinheiro(double entradaDinheiro) {
        validarNaoNegativo(entradaDinheiro, "Entrada Dinheiro");
        this.entradaDinheiro = entradaDinheiro;
    }

    public double getTrocoInicial() { return trocoInicial; }
    public void setTrocoInicial(double trocoInicial) {
        validarNaoNegativo(trocoInicial, "Troco Inicial");
        this.trocoInicial = trocoInicial;
    }

    private void validarNaoNegativo(double v, String nome) {
        if (v < 0) throw new IllegalArgumentException(nome + " não pode ser negativo.");
    }

    // totais por tipo calculados nas máquinas
    public double totalCreditoMaquinas() {
        return maquinas.stream().mapToDouble(MaquinaCartao::getCredito).sum();
    }
    public double totalDebitoMaquinas() {
        return maquinas.stream().mapToDouble(MaquinaCartao::getDebito).sum();
    }
    public double totalPixMaquinas() {
        return maquinas.stream().mapToDouble(MaquinaCartao::getPix).sum();
    }

    public double getTotalCredito() { return totalCreditoMaquinas(); }
    public double getTotalDebito() { return totalDebitoMaquinas(); }
    public double getTotalPix() { return totalPixMaquinas(); }

    // diferenças
    public double diferencaCredito() {
        return totalCreditoMaquinas() - relatorioCredito;
    }
    public double diferencaDebito() {
        return totalDebitoMaquinas() - relatorioDebito;
    }
    public double diferencaPix() {
        return totalPixMaquinas() - relatorioPix;
    }
    public double diferencaDinheiro() {
        return entradaDinheiro - relatorioDinheiro - trocoInicial;
    }

    public double getDiferencaCredito() { return diferencaCredito(); }
    public double getDiferencaDebito() { return diferencaDebito(); }
    public double getDiferencaPix() { return diferencaPix(); }
    public double getDiferencaDinheiro() { return diferencaDinheiro(); }

    // NOVO MÉTODO: cálculo final de dinheiro (independente do turno)
    public double resultadoDinheiroFinal() {
        return entradaDinheiro - trocoInicial - relatorioDinheiro;
    }

    // persistência
    public List<String> toBlockLines() {
        List<String> out = new ArrayList<>();
        out.add("Responsável:" + escape(responsavel == null ? "" : responsavel));
        out.add("Data:" + (data == null ? "" : data.format(DATE_FMT)));
        out.add("Turno:" + turno);
        out.add("Máquinas:" + maquinas.size());
        for (MaquinaCartao m : maquinas) {
            out.add("Máquina:" + m.toLine());
        }
        out.add(String.format("Relatório crédito:%.2f", relatorioCredito));
        out.add(String.format("Relatório débito:%.2f", relatorioDebito));
        out.add(String.format("Relatório pix:%.2f", relatorioPix));
        out.add(String.format("Dinheiro em caixa:%.2f", entradaDinheiro));
        out.add(String.format("Relatório dinheiro:%.2f", relatorioDinheiro));
        out.add(String.format("Troco:%.2f", trocoInicial));
        out.add("Fim");
        return out;
    }

    public static Fechamento fromBlockLines(List<String> block) {
        Fechamento f = new Fechamento();
        try {
            int i = 0;
            while (i < block.size()) {
                String ln = block.get(i);
                if (ln.startsWith("Responsável:")) {
                    f.setResponsavel(unescape(ln.substring(5)));
                } else if (ln.startsWith("Data:")) {
                    String dateStr = ln.substring(5);
                    if (!dateStr.isBlank()) {
                        f.setData(LocalDate.parse(dateStr, DATE_FMT));
                    }
                } else if (ln.startsWith("Turno:")) {
                    String t = ln.substring(6);
                    if (!t.isBlank()) f.setTurno(Integer.parseInt(t));
                } else if (ln.startsWith("Máquinas:")) {
                    int n = Integer.parseInt(ln.substring(9));
                    i++;
                    for (int m = 0; m < n && i < block.size(); m++, i++) {
                        String ml = block.get(i);
                        if (!ml.startsWith("Máquina:")) throw new IllegalArgumentException("Máquina esperada: " + ml);
                        MaquinaCartao maq = MaquinaCartao.fromLine(ml.substring(8));
                        f.addMaquina(maq);
                    }
                    i--;
                } else if (ln.startsWith("Relatório crédito:")) {
                    f.setRelatorioCredito(Double.parseDouble(ln.substring(7)));
                } else if (ln.startsWith("Relatório débito:")) {
                    f.setRelatorioDebito(Double.parseDouble(ln.substring(6)));
                } else if (ln.startsWith("Relatório pix:")) {
                    f.setRelatorioPix(Double.parseDouble(ln.substring(6)));
                } else if (ln.startsWith("Dinheiro entrada:")) {
                    f.setEntradaDinheiro(Double.parseDouble(ln.substring(12)));
                } else if (ln.startsWith("Relatório dinheiro:")) {
                    f.setRelatorioDinheiro(Double.parseDouble(ln.substring(6)));
                } else if (ln.startsWith("Troco:")) {
                    f.setTrocoInicial(Double.parseDouble(ln.substring(6)));
                } else if (ln.startsWith("Fim")) {
                    break;
                }
                i++;
            }
            return f;
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao parsear bloco: " + e.getMessage(), e);
        }
    }

    private static String escape(String s) { return s.replace("\n", " ").replace("\r", " ").trim(); }
    private static String unescape(String s) { return s; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n===== FECHAMENTO =====\n");
        sb.append("Responsável: ").append(responsavel == null ? "" : responsavel).append("\n");
        sb.append("Data: ").append(data == null ? "" : data.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))).append("\n");
        sb.append("Turno: ").append(turno == 1 ? "Manhã" : "Tarde/Noite").append("\n\n");

        sb.append("=== Máquinas ===\n");
        int idx = 1;
        for (MaquinaCartao m : maquinas) {
            sb.append(String.format("Máquina %d: %s\n", idx++, m.toString()));
        }

        sb.append("\n=== Relatórios informados ===\n");
        sb.append(String.format("Crédito (rel): R$ %.2f | Máquinas (líq): R$ %.2f | Dif: R$ %.2f\n",
                relatorioCredito, totalCreditoMaquinas(), diferencaCredito()));
        sb.append(String.format("Débito  (rel): R$ %.2f | Máquinas (líq): R$ %.2f | Dif: R$ %.2f\n",
                relatorioDebito, totalDebitoMaquinas(), diferencaDebito()));
        sb.append(String.format("Pix     (rel): R$ %.2f | Máquinas : R$ %.2f | Dif: R$ %.2f\n",
                relatorioPix, totalPixMaquinas(), diferencaPix()));

        sb.append("\n=== Dinheiro ===\n");
        sb.append(String.format("Dinheiro em caixa: R$ %.2f | Relatório: R$ %.2f | Dif: R$ %.2f\n",
                entradaDinheiro, relatorioDinheiro, diferencaDinheiro()));
        sb.append(String.format("Troco inicial: R$ %.2f\n", trocoInicial));

        sb.append(String.format("Resultado final (dinheiro - troco - rel): R$ %.2f\n",
                resultadoDinheiroFinal()));

        sb.append("====================\n");
        return sb.toString();
    }
}
