package coder.naza.map.download;

import java.util.Collections;

public class ConsoleOutHelper {
    public static void main(String[] arg) {
        try {
            for (int i = 0; i <= 100; i++) {
                backSpace(30 * 2 + 10);
                String bar = progressbar(i, 30) + " " + i + "%";

                System.out.print(bar);

                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * 回退N个字符
     * @param charsnum 回退字符数
     * @return:
     */
    public static void backSpace(int charsnum) {
        for (int j = 0; j < charsnum; j++) {
            System.out.print("\b");
        }
    }
    public static void home(){
        System.out.print("\r");
    }
    /**
     * 生成进度条，注意：每个方块占2个字符长度。
     * @param per 进度百分比
     * @param len 进度条长度（方块数量），一个方块占2个字符长度。
     * @return:进度条字符串
     */
    public static String progressbar(int per, int len) {
        if (len < 2)
            len = 2;
        int s = len * per / 100;
        String barstr = String.join("", Collections.nCopies(s, "■"));
        barstr += String.join("", Collections.nCopies(len - s, "□"));
        return barstr;
    }
}