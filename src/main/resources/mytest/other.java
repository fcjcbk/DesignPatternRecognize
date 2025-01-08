package example.b;
import java.util.ArrayList;

class Foo {

}
class Good {
    // 成员变量
    ArrayList<Foo> list = new ArrayList<>();
    public void add(int num) {
        // 局部变量
        ArrayList<Integer> list = new ArrayList<>();
        // 调用局部变量的add
        list.add(num);
    }

    public void add2() {
        // 调用成员变量的add
        list.add(new Foo());
    }
}