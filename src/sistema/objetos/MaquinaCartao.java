package sistema.objetos;

/**
 * Máquina de cartão (sem nome). Valores por tipo.
 */
public class MaquinaCartao {

    private double credito;
    private double debito;
    private double pix;

    public MaquinaCartao() {}

    public MaquinaCartao(double credito, double debito, double pix) {
        setCredito(credito);
        setDebito(debito);
        setPix(pix);
    }

    public double getCredito() { return credito; }
    public void setCredito(double credito) {
        validarValor(credito, "Crédito");
        this.credito = credito;
    }

    public double getDebito() { return debito; }
    public void setDebito(double debito) {
        validarValor(debito, "Débito");
        this.debito = debito;
    }

    public double getPix() { return pix; }
    public void setPix(double pix) {
        validarValor(pix, "Pix");
        this.pix = pix;
    }

    private void validarValor(double valor, String campo) {
        if (valor < 0) {
            throw new IllegalArgumentException(campo + " não pode ser negativo.");
        }
    }

    public double getTotal() {
        return credito + debito + pix;
    }

    @Override
    public String toString() {
        return String.format("Crédito: R$ %.2f | Débito: R$ %.2f | Pix: R$ %.2f",
                credito, debito, pix);
    }

    // serialização simples: credito|debito|pix
    public String toLine() {
        return String.format("%.2f|%.2f|%.2f", credito, debito, pix);
    }

    public static MaquinaCartao fromLine(String line) {
        String[] p = line.split("\\|", -1);
        if (p.length != 3)
            throw new IllegalArgumentException("Linha de máquina inválida: " + line);

        double c = Double.parseDouble(p[0]);
        double d = Double.parseDouble(p[1]);
        double px = Double.parseDouble(p[2]);

        return new MaquinaCartao(c, d, px);
    }
}
