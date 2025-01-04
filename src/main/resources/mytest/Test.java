package packageName;
class compose {

}

class aggre {

}

class asso {

}

interface testt {

}

class ab {
    public static void staticFunc() {

    }
}

class DependencyVariable {

}

class ArrayType {

}

class Tt extends ab implements testt {
    private compose compose;
    private aggre aggre;
    private asso asso;
    private ArrayType[] arrayTypes;

    public Tt() {
        compose = new compose();
    }

    public void setAggre(aggre aggre) {
        this.aggre = aggre;
        ab.staticFunc();

        DependencyVariable dependencyVariable = new DependencyVariable();
    }
}
