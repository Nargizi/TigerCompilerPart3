public class ArrayElementArgument implements Argument{
    private Argument arr;
    private Argument index;

    public ArrayElementArgument(Argument arr, Argument index) {
        this.arr = arr;
        this.index = index;
    }

    public Argument getArr() {
        return arr;
    }

    public Argument getIndex() {
        return index;
    }

    @Override
    public String getValue() {
        return arr.getValue();
    }

    @Override
    public void setValue(String value) {
        arr.setValue(value);
    }

    @Override
    public Register getRegister() {
        return arr.getRegister();
    }

    @Override
    public void setRegister(Register register) {
        arr.setRegister(register);
    }

    @Override
    public Integer getSize() {
        return arr.getSize();
    }

    @Override
    public void setSize(Integer size) {
        arr.setSize(size);
    }

    @Override
    public boolean isEmpty() {
        return false;
    }
}
