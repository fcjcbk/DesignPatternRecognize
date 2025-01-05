package packageName;

import java.util.ArrayList;
import java.util.HashMap;

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
    private ArrayList<testt> list;

    public Tt(ArrayType[] a1, ArrayList<testt> f1, HashMap<String, testt> ff) {
        compose p = new compose();
        compose = p;
        arrayTypes = a1;
        list = new ArrayList<>();
    }

    public void setAggre(aggre aggre) {
        this.aggre = aggre;
        ab.staticFunc();

        DependencyVariable dependencyVariable = new DependencyVariable();
    }
}
