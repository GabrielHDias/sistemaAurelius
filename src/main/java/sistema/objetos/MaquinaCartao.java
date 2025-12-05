package main.java.sistema.objetos;

public class MaquinaCartao {

    private double credito;
    private double debito;
    private double pix;

    // Construtor vazio (requerido para carregamento)
    public MaquinaCartao() {}

    // Construtor usado no Principal
    public MaquinaCartao(double credito, double debito, double pix) {
        this.credito = credito;
        this.debito = debito;
        this.pix = pix;
    }

    public double getCredito() {
        return credito;
    }

    public void setCredito(double credito) {
        this.credito = credito;
    }

    public double getDebito() {
        return debito;
    }

    public void setDebito(double debito) {
        this.debito = debito;
    }

    public double getPix() {
        return pix;
    }

    public void setPix(double pix) {
        this.pix = pix;
    }

    public double getTotal() {
        return credito + debito + pix;
    }

    @Override
    public String toString() {
        return "Crédito: " + credito +
                ", Débito: " + debito +
                ", Pix: " + pix +
                ", Total Máq: " + getTotal();
    }

    // Para salvar em arquivo
    public String toLine() {
        return credito + ";" + debito + ";" + pix;
    }

    // Para carregar do arquivo
    public static MaquinaCartao fromLine(String line) {
        String[] p = line.split(";");
        return new MaquinaCartao(
                Double.parseDouble(p[0]),
                Double.parseDouble(p[1]),
                Double.parseDouble(p[2])
        );
    }
}
