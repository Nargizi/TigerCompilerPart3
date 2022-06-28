import org.antlr.v4.runtime.CharStream;

import java.util.Objects;

public interface Argument {
    String getValue();
    void setValue(String value);
    Register getRegister();
    void setRegister(Register register);
    Integer getSize();
    void setSize(Integer size);
    boolean isEmpty();
}

class IntArgument implements Argument {
    private String value;
    private Integer size;
    private Register register;
    private boolean isEmpty;

    public IntArgument(String value, Integer size){
        this.value = value;
        this.size = size;
        this.register = null;
        checkState();
    }

    public IntArgument(String value){
        this(value, 1);
    }

    private void checkState(){
        this.isEmpty = (value == null) || (value.isBlank());
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public Register getRegister() {
        return register;
    }

    @Override
    public void setRegister(Register register) {
        this.register = register;
    }

    @Override
    public Integer getSize() {
        return size;
    }

    @Override
    public void setSize(Integer size) {
        this.size = size;
    }
    

    @Override
    public boolean isEmpty() {
        return isEmpty;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Argument argument = (Argument) o;
        return Objects.equals(value, argument.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}

class FloatArgument implements Argument {
    private String value;
    private Integer size;
    private Register register;
    private boolean isEmpty;

    public FloatArgument(String value, Integer size){
        this.value = value;
        this.size = size;
        this.register = null;
        checkState();
    }

    public FloatArgument(String value){
        this(value, 1);
    }

    private void checkState(){
        this.isEmpty = (value == null) || (value.isBlank());
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public Register getRegister() {
        return register;
    }

    @Override
    public void setRegister(Register register) {
        this.register = register;
    }

    @Override
    public Integer getSize() {
        return size;
    }

    @Override
    public void setSize(Integer size) {
        this.size = size;
    }

    @Override
    public boolean isEmpty() {
        return isEmpty;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Argument argument = (Argument) o;
        return Objects.equals(value, argument.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

}

class ConstantArgument implements Argument{
    private String value;

    public ConstantArgument(String value){
        this.value = value;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public void setValue(String value) {

    }

    @Override
    public Register getRegister() {
        return null;
    }

    @Override
    public void setRegister(Register register) {

    }

    @Override
    public Integer getSize() {
        return null;
    }

    @Override
    public void setSize(Integer size) {

    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
