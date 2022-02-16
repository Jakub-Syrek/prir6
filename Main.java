public class Main {

    private static int GetChunk(int num){
        double s = num / 2.0;
        String dbl = Double.toString(s);
        String res = dbl.substring(0,dbl.indexOf("."));
        return Integer.valueOf(res);
    }
    public static void main(String[] args) {
        System.out.println(GetChunk(0));
        System.out.println(GetChunk(1));
        System.out.println(GetChunk(2));
        System.out.println(GetChunk(3));

    }
}
