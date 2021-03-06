package java.net;

public class URISyntaxException extends Exception {
    private static final long serialVersionUID = 2137979680897488891L;
    private int index;
    private String input;

    public URISyntaxException(String input, String reason, int index) {
        super(reason);
        if (input == null || reason == null) {
            throw new NullPointerException();
        } else if (index < -1) {
            throw new IllegalArgumentException();
        } else {
            this.input = input;
            this.index = index;
        }
    }

    public URISyntaxException(String input, String reason) {
        this(input, reason, -1);
    }

    public String getInput() {
        return this.input;
    }

    public String getReason() {
        return super.getMessage();
    }

    public int getIndex() {
        return this.index;
    }

    public String getMessage() {
        StringBuffer sb = new StringBuffer();
        sb.append(getReason());
        if (this.index > -1) {
            sb.append(" at index ");
            sb.append(this.index);
        }
        sb.append(": ");
        sb.append(this.input);
        return sb.toString();
    }
}
