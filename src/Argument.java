import java.util.Objects;


public interface Argument {

}

enum Type {
    Float(8, "float"),
    Integer(4, "int");


    private final Integer size;
    private final String name;

    Type(Integer size, String name) {
        this.size = size;
        this.name = name;
    }

    public Integer getSize() {
        return size;
    }

    @Override
    public String toString() {
        return name;
    }


}

class Constant implements Argument {
    private String value;

    public Constant(String value){
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Constant constant = (Constant) o;
        return Objects.equals(value, constant.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

class Variable implements Argument {
    private String name;
    private Type type;

    public Variable(String name, Type type){
        this.name = name;
        this.type = type;
    }

    public Type getType(){
        return type;
    }

    public String getName(){
        return name;
    }

    public Integer getSize() {return type.getSize();}

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Variable variable = (Variable) o;
        return Objects.equals(name, variable.name) && type == variable.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type);
    }

    @Override
    public String toString() {
        return name;
    }
}

class Register extends Variable{

    public Register(String name, Type type) {
        super(name, type);
    }

    @Override
    public String toString() {
        return "$" + getName();
    }
}

class Array extends Variable {
    private Integer size;

    public Array(String name, Type type, Integer size){
        super(name, type);
        this.size = size;
    }

    public Integer getSize() {
        return size * super.getSize();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        if (!super.equals(o))
            return false;
        Array array = (Array) o;
        return Objects.equals(size, array.size);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), size);
    }

    @Override
    public String toString() {
        return getName();
    }
}
