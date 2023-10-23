package coder.naza.map.download;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.zip.GZIPInputStream;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class MapDownload {

    private static Scanner scanner;

    public static void main(String[] arg) throws IOException {
        boolean ret;
        scanner = new Scanner(System.in);
        inputStorePath();
        while (true) {
            ret = downloadStyle();
            if (ret) {
                System.out.println("下载成功，开始解析！");
            } else {
                System.out.println("下载失败，任务中止！");
                outerror();
                return;
            }
            parseStyle();
            downloadSprite();
            
            downloadFont();
            outputSamples();
            ret = outerror();
            errorMap.clear();
            if (!ret) {
                for (int t = 15; t > 0; t--) {
                    System.out.print("["+t + "]秒后开始重新下载失败文件，按Ctrl+C中止...    ");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    ConsoleOutHelper.home();
                }
                System.out.println("");
            }else{
                System.out.print("按回车键结束...");
                scanner.nextLine();
                break;
            }
        }
    }

    public static void selectHost() {
        System.out.print("请选择下载站点：\n0-https://api.mapbox.com\n1-https://api-global.mapbox.cn\n2-http://39.105.194.166:8088\n3-输入服务器地址\n:");
        acctoken = scanner.nextLine();
        int i=Integer.parseInt(acctoken);
        while (i!=0 && i!=1 && i!=2 && i!=3) {
            System.out.print("无效选择，请重新选择：");
            acctoken = scanner.nextLine();
            i=Integer.parseInt(acctoken);
        }
        setHost(i);
    }

    public static void selecVecType() {
        System.out.print("请选择下载矢量数据类型：\n0-基础(默认)\n1-(基础+地形)\n2-(基础+地形+海洋)\n:");
        acctoken = scanner.nextLine();
        int i=Integer.parseInt(acctoken);
        while (i!=0 && i!=1 && i!=2) {
            System.out.print("无效选择，请重新选择：");
            i=Integer.parseInt(acctoken);
        }
        setVercorTyle(i);
    }
    public static String host ="https://api.mapbox.com";

    public static String vecrtorType ="mapbox.mapbox-streets-v8";

    public static Boolean ProxyMap = false;
    public static void setHost(int i){
        switch(i){
            case 0:
                host="https://api.mapbox.com";
                break;
            case 1:
                host="https://api-global.mapbox.cn";
                break;
            case 2:
                host="http://39.105.194.166:8088";
                ProxyMap = true;
                break;
            case 3:
                System.out.print("请输入地址()：");
                acctoken = scanner.nextLine();
                host = acctoken;
                break;
            default:
                break;
        }
    }

    public static void setVercorTyle(int i){
        switch(i){
            case 0:
                vecrtorType = "mapbox.mapbox-streets-v8";
                break;
            case 1:
                vecrtorType = "mapbox.mapbox-streets-v8,mapbox.mapbox-terrain-v2";
                break;
            case 2:
                vecrtorType = "mapbox.mapbox-streets-v8,mapbox.mapbox-terrain-v2,mapbox.mapbox-bathymetry-v2";
                break;
        }
    }
    private static String acctoken = "";

    public static void inputAccessToken() {
        System.out.print("请输入AccessToken(以pk.开始)：");
        acctoken = scanner.nextLine();
        while (!acctoken.startsWith("pk.")) {
            System.out.print("无效Token，请重新输入AccessToken(以pk.开始)：");
            acctoken = scanner.nextLine();
        }
    }

    private static URL getAccessUrl(String url) throws MalformedURLException {
        return new URL(url + "?access_token=" + acctoken);
    }

    private static String storepath = "";

    public static void inputStorePath() {
        System.out.print("请输入保存目录：");
        storepath = scanner.nextLine();
        if (!storepath.endsWith("/"))
            storepath += "/";
        File file = new File(storepath);
        while (file.exists()) {
            System.out.print("目录已经存在，是否载入原工程？（Y/N)：");
            String yn = scanner.nextLine();
            if ("y".equalsIgnoreCase(yn)) {
                loadProfile();
                return;
            }
            System.out.print("请重新输入保存目录：");
            storepath = scanner.nextLine();
            if (!storepath.endsWith("/"))
                storepath += "/";
            file = new File(storepath);
        }
        newProfile();
    }

    public static void newProfile() {
        selectHost();
        // selecVecType();
        inputAccessToken();
        inputStylePath();
        inputBound();
        inputZoom();
        JSONObject object = new JSONObject();
        // string
        object.put("accessToken", acctoken);
        // int
        object.put("stylePath", stylePath);
        object.put("host",host);
        List<Double> lst = Arrays.asList(bound[0], bound[1], bound[2], bound[3]);
        object.put("bound", lst);
        object.put("zoom", zoom);

        writeFileContent(storepath + "profile.json", object.toJSONString());
    }

    public static void loadProfile() {
        String str = readFileContent(storepath + "profile.json");
        JSONObject object = JSONObject.parseObject(str);
        acctoken = object.getString("accessToken");
        stylePath = object.getString("stylePath");
        host=object.getString("host");
        if(null==host || "".equals(host)){
            setHost(0);
        }
        List<Double> lst = JSON.parseArray(object.getJSONArray("bound").toJSONString(), Double.class);
        bound[0] = lst.get(0);
        bound[1] = lst.get(1);
        bound[2] = lst.get(2);
        bound[2] = lst.get(2);
        List<Integer> zoomlst = JSON.parseArray(object.getJSONArray("zoom").toJSONString(), Integer.class);
        zoom[0] = zoomlst.get(0);
        zoom[1] = zoomlst.get(1);
    }

    private static final String PREFIX_MAPBOX = "mapbox://";
    private static String stylePath = "";

    public static void inputStylePath() {
        System.out.print("请输入Style下载路径(以" + PREFIX_MAPBOX + "开始)：");
        stylePath = scanner.nextLine();
        while (!stylePath.startsWith(PREFIX_MAPBOX)) {
            System.out.print("无效路径，请输入Style下载路径(以" + PREFIX_MAPBOX + "开始)：");
            stylePath = scanner.nextLine();
        }
    }

    public static double[] bound = { 119.5, 31.1, 120.6, 32.0 };// 无锡

    public static void inputBound() {
        System.out.print("请输入地图边界经纬度范围(以,分隔，顺序为：左经,下纬，右经,上纬)：");
        String str = scanner.nextLine();
        while (!parseBound(str)) {
            System.out.print("无效! 请重新输入地图边界经纬度范围(以,分隔，顺序为：左经,下纬，右经,上纬)：");
            str = scanner.nextLine();
        }
    }

    private static boolean parseBound(String str) {
        try {
            String nums[] = str.split(",");
            if (nums.length < 4)
                return false;
            bound[0] = Double.parseDouble(nums[0]);
            bound[1] = Double.parseDouble(nums[1]);
            bound[2] = Double.parseDouble(nums[2]);
            bound[3] = Double.parseDouble(nums[3]);
            return true;
        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    public static void inputZoom() {
        System.out.print("请输入地图缩放范围(以,分隔的从小到大两个数字，值范围0-16)：");
        String str = scanner.nextLine();
        while (!parseZoom(str)) {
            System.out.print("无效! 请重新输入地图缩放范围(以,分隔的从小到大两个数字，值范围0-16)：");
            str = scanner.nextLine();
        }
    }

    private static int zoom[] = { 0, 16 };

    private static boolean parseZoom(String str) {
        try {
            String nums[] = str.split(",");
            if (nums.length < 2)
                return false;
            zoom[0] = Integer.parseInt(nums[0]);
            zoom[1] = Integer.parseInt(nums[1]);
            if (zoom[0] >= 0 && zoom[0] <= 16 && zoom[1] >= 0 && zoom[1] <= 16 && zoom[0] <= zoom[1])
                return true;
            else
                return false;

        } catch (Exception e) {
            System.out.println(e);
        }
        return false;
    }

    private static String spriteurl;
    private static String glyphsurl;
    private static Map<String, String> glyphstack = new HashMap<String, String>();

    private static boolean parseStyle() {
        String str = readFileContent(storepath + "src/style.json");
        JSONObject obj = JSONObject.parseObject(str);
        // "sprite":"mapbox://sprites/timm-tsy/ck4s8nck60x4e1cqru8kovn48/5ottmet3xicndljtew0qjwj8y",
        // "glyphs": "mapbox://fonts/timm-tsy/{fontstack}/{range}.pbf",parseStyle
        spriteurl = obj.getString("sprite");
        obj.put("sprite", "/sprites/sprite");
        glyphsurl = obj.getString("glyphs");
        obj.put("glyphs", "/fonts/{fontstack}/{range}.pbf");
        // 解析字体组合
        parseFontStack(obj.getJSONArray("layers"));
        parseSource(obj.getJSONObject("sources"));
        // 输出本地化style.json
        writeFileContent(storepath + "style.json", obj.toJSONString());
        return true;
    }

    public static void parseFontStack(JSONArray layers) {
        for (int i = 0; i < layers.size(); i++) {
            JSONObject layer = layers.getJSONObject(i);
            if (layer.containsKey("layout")) {
                JSONObject layout = layer.getJSONObject("layout");
                if (layout.containsKey("text-font")) {
                    Object textfont = layout.get("text-font");
                    String stackString = "";
                    if (textfont instanceof JSONArray) {
                        // "text-font": [
                        // "DIN Offc Pro Regular",
                        // "Arial Unicode MS Regular"
                        // ],
                        JSONArray stacks = (JSONArray) textfont;
                        stackString = stacks.getString(0) + "," + stacks.getString(1);
                        glyphstack.put(stackString, glyphsurl);
                    } else {
                        // "text-font": {
                        // "base": 1,
                        // "stops": [
                        // [
                        // 11,["DIN Offc Pro Regular","Arial Unicode MS Regular"]
                        // ],
                        // [
                        // 12,["DIN Offc Pro Medium","Arial Unicode MS Regular"]
                        // ]
                        // ]
                        // },
                        JSONObject fontobj = (JSONObject) textfont;
                        JSONArray stops = fontobj.getJSONArray("stops");
                        for (int j = 0; j < stops.size(); j++) {
                            JSONArray stop = stops.getJSONArray(j);
                            JSONArray stacks = stop.getJSONArray(1);
                            stackString = stacks.getString(0) + "," + stacks.getString(1);
                            glyphstack.put(stackString, glyphsurl);
                        }
                    }

                }
            }
        }
    }

    public static void parseSource(JSONObject source) {
        // "sources": {
        // "mapbox://mapbox.mapbox-traffic-v1": {
        // "url": "mapbox://mapbox.mapbox-traffic-v1",
        // "type": "vector"
        // },
        // "mapbox://mapbox.mapbox-incidents-v1": {
        // "url": "mapbox://mapbox.mapbox-incidents-v1",
        // "type": "vector"
        // },
        // "composite": {
        // "url":
        // "mapbox://https://api.mapbox.com/v4/mapbox.mapbox-streets-v8/12/3370/1548.vector.pbf?sku=1011ViqlY51c0&access_token=pk.eyJ1IjoiamFleW91ZSIsImEiOiJjbG4xNWFzdXQxajF0MmpuemRsM3kzYnFuIn0.RS4t4k_LoDBu5HOyJ-uylw,mapbox.mapbox-terrain-v2,timm-tsy.ck2wki7p30clh2ilw0xvpbx6i-30v30",
        // "type": "vector"
        // }
        // },
        for (Map.Entry<String, Object> entry : source.entrySet()) {// 一个entry代表一个属性，key是属性名，value是属性值
            JSONObject obj = (JSONObject) entry.getValue();
            String key = entry.getKey();
            key = key.replace(PREFIX_MAPBOX, "");
            String srcurl = obj.getString("url") + ".json";
            String path = "source/" + key;
            String type = obj.getString("type");

            // 下载source描述文件，并存为source.json
            String jsonfilename = "/" + srcurl.replace(PREFIX_MAPBOX, "");
            srcurl = srcurl.replace(PREFIX_MAPBOX, host+"/v4/");

            try {
                URL url = getAccessUrl(srcurl);
                File file = new File(storepath + path + jsonfilename);
                System.out.print("正在下载" + key + "描述文件...");
                boolean ret;
                ret = download("source:" + key, url, file);
                if (!ret) {
                    System.out.println("Error!");
                    file.delete();
                    System.out.println(url);
                } else {
                    System.out.println("Completed!");
                }

                parseTiles(path, jsonfilename, type);
            } catch (Exception e) {
                e.printStackTrace();
            }
            obj.put("url", "/" + path + jsonfilename);
        }
    }

    public static void parseTiles(String path, String jsonfilename, String type) throws IOException {
        // "tiles": [
        // "http://a.tiles.mapbox.com/v4/mapbox.mapbox-terrain-v2,mapbox.mapbox-streets-v7/{z}/{x}/{y}.vector.pbf?access_token=pk.eyJ1IjoiZmljdGlvbmtpbmciLCJhIjoiY2s0OTkwdnZqMDFzMjNrbXczbWVrMmkzbyJ9.VZfDF5h7nHdfX58bCZM3yw",
        // "http://b.tiles.mapbox.com/v4/mapbox.mapbox-terrain-v2,mapbox.mapbox-streets-v7/{z}/{x}/{y}.vector.pbf?access_token=pk.eyJ1IjoiZmljdGlvbmtpbmciLCJhIjoiY2s0OTkwdnZqMDFzMjNrbXczbWVrMmkzbyJ9.VZfDF5h7nHdfX58bCZM3yw"
        // ],
        String str = readFileContent(storepath + path + jsonfilename);
        JSONObject obj = JSONObject.parseObject(str);
        JSONArray tiles = obj.getJSONArray("tiles");
        double sourcebounds[] = { 0, 0, 0, 0 };
        List<Double> lst = JSON.parseArray(obj.getJSONArray("bounds").toJSONString(), Double.class);
        sourcebounds[0] = lst.get(0);
        sourcebounds[1] = lst.get(1);
        sourcebounds[2] = lst.get(2);
        sourcebounds[3] = lst.get(3);
        if (tiles.isEmpty())
            return;
        String url = tiles.getString(0);
        String filetype;
        if ("vector".equals(type)) {
            filetype = ".pbf";
        } else {
            filetype = ".png";
        }
        downloadTile(storepath + path + "/", url, filetype, sourcebounds);
        tiles = new JSONArray();
        tiles.add("/" + path + "/tiles/{z}/{x}/{y}" + filetype);
        obj.put("tiles", tiles);
        obj.put("attribution", "");
        obj.put("mapbox_logo", false);
        str = obj.toJSONString();
        writeFileContent(storepath + path + jsonfilename, str);
    }

    public static boolean downloadSprite() {
        System.out.println("开始下载图标...");
        String urlpath = spriteurl.replace(PREFIX_MAPBOX + "sprites/", host+"/styles/v1/");
        try {
            boolean ret;
            URL url = getAccessUrl(urlpath + "/sprite.json");
            File file = new File(storepath + "sprites/sprite.json");
            System.out.print("正在下载sprite.json...");
            if (!file.exists()) {
                ret = download("sprite json", url, file);
                if (!ret) {
                    System.out.println("Error!");
                    file.delete();
                    System.out.println(url);
                } else {
                    System.out.println("Completed!");
                }
            } else {
                System.out.println("Completed!");
            }

            url = getAccessUrl(urlpath + "/sprite.png");
            file = new File(storepath + "sprites/sprite.png");
            System.out.print("正在下载sprite.png...");
            if (!file.exists()) {
                ret = download("sprite png", url, file);
                if (!ret) {
                    System.out.println("Error!");
                    file.delete();
                    System.out.println(url);
                } else {
                    System.out.println("Completed!");
                }
            } else {
                System.out.println("Completed!");
            }
            url = getAccessUrl(urlpath + "/sprite@2x.json");
            file = new File(storepath + "sprites/sprite@2x.json");
            System.out.print("正在下载sprite@2x.json...");
            if (!file.exists()) {
                ret = download("sprite 2x json", url, file);
                if (!ret) {
                    System.out.println("Error!");
                    file.delete();
                    System.out.println(url);
                } else {
                    System.out.println("Completed!");
                }
            } else {
                System.out.println("Completed!");
            }
            url = getAccessUrl(urlpath + "/sprite@2x.png");
            file = new File(storepath + "sprites/sprite@2x.png");
            System.out.print("正在下载sprite@2x.png...");
            if (!file.exists()) {
                ret = download("sprite x2 png", url, file);
                if (!ret) {
                    System.out.println("Error!");
                    file.delete();
                    System.out.println(url);
                } else {
                    System.out.println("Completed!");
                }
            } else {
                System.out.println("Completed!");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean downloadStyle() {
        String urlpath = stylePath.replace(PREFIX_MAPBOX + "styles/", host+"/styles/v1/");
        try {
            URL url = getAccessUrl(urlpath);
            File stylefile = new File(storepath + "src/style.json");
            System.out.println("正在下载Style主文件["+url+"]...");
            if (!stylefile.exists()) {
                System.out.println(url);
                boolean ret = download("stylefile", url, stylefile);
                return ret;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static class TileTask extends Thread {
        public String srcurl;
        public int z;
        public int x;
        public int y;
        public String filetype;
        public String tilepath;
        public Semaphore semaphore;

        @Override
        public void run() {
            try {

                String urlstr = srcurl;
                urlstr = urlstr.replace("{z}", String.valueOf(z));
                urlstr = urlstr.replace("{x}", String.valueOf(x));
                urlstr = urlstr.replace("{y}", String.valueOf(y));
                if (ProxyMap){
                    urlstr = urlstr.replace("http://a.tiles.mapbox.com", host + "/api");
                }else{
                    urlstr = urlstr.replace("http", "https");
                }

                String path = null;
                // if (type.equals("ArcGIS")) {
                // //ArcGIS格式瓦片下载
                // path = getTDTilesForArcGISPath(x, y, z);
                // } else {
                // //一般格式瓦片下载
                // path = getTDTilesForCustomPath(x, y, z);
                // }
                // path = getTDTilesForCustomPathMvt(x,y,z);
                path = tilepath + getOpenstreetForCustomPathMvt(x, y, z) + filetype;

                File file = new File(path);
                if (file.exists()) {
                    increase();
                } else {
                    File tmpfile = new File(path + ".tmp");
                    URL url = new URL(urlstr);
                    String zxyStr = "Tile:" + z + "/" + x + "/" + y;
                    Boolean ret = download(zxyStr, url, tmpfile);
                    if (ret) {
                        if (".pbf".equals(filetype)) {
                            if (ProxyMap){
                                uncompress(tmpfile, file);
                                tmpfile.delete();
                                // tmpfile.renameTo(file);
                            }else{
                                uncompress(tmpfile, file);
                                tmpfile.delete();
                            }
                        } else {
                            tmpfile.renameTo(file);
                        }
                        increase();
                    } else {
                        tmpfile.delete();
                    }
                }
            } catch (Exception e) {

            }
            semaphore.release();
        }
    }

    public static void downloadTile(String tilepath, String srcurl, String filetype, double sourcebounds[])
            throws IOException {
        // double[] bound = {68, 11, 135, 54};//中国
        // double[] bound = {113, 23, 129, 35.26};//华东（广州-釜山）
        // double[] bound = {117, 26, 123, 32};//浙江

        System.out.println("开始下载瓦片...");
        int threadsize = 99;
        Semaphore semaphore = new Semaphore(threadsize);
        for (int z = zoom[0]; z <= zoom[1]; z++) {
            // 计算行列号(使用瓦片的中心点经纬度计算)
            double worldSize = Math.pow(2, z);

            int minC = (int) Math.floor(mercatorXfromLng(Math.max(sourcebounds[0], bound[0])) * worldSize);
            int minR = (int) Math.floor(mercatorYfromLat(Math.min(sourcebounds[3], bound[3])) * worldSize);
            int maxC = (int) Math.ceil(mercatorXfromLng(Math.min(sourcebounds[2], bound[2])) * worldSize);
            int maxR = (int) Math.ceil(mercatorYfromLat(Math.max(sourcebounds[1], bound[1])) * worldSize);

            // 起始结束行

            // int minR = getOSMTileYFromLatitude(Math.min(sourcebounds[3], bound[3]), z);
            // int maxR = getOSMTileYFromLatitude(Math.max(sourcebounds[1], bound[1]), z);
            // // 起始结束列
            // int minC = getOSMTileXFromLongitude(Math.max(sourcebounds[0], bound[0]), z);
            // int maxC = getOSMTileXFromLongitude(Math.min(sourcebounds[2], bound[2]), z);
            System.out.println("第" + z + "级总共将会有：" + (maxC - minC) + "个文件夹，每个有" + (maxR - minR) + "个文件;总下载："
                    + (maxC - minC) * (maxR - minR) + "个文件：");
            int count = (maxR - minR) * (maxC - minC);
            printProgressBar(0, barlen);
            countdown = 0;
            for (int y = minR; y < maxR; y++) {
                for (int x = minC; x < maxC; x++) {
                    // String urlstr = "https://maps.tilehosting.com/data/v3/" + z + "/" + x + "/" +
                    // y +".pbf?key=yzgDrAunRjJjwhG6D3u7";
                    // String urlstr =
                    // "http://a.tiles.mapbox.com/v4/mapbox.mapbox-terrain-v2,mapbox.mapbox-streets-v7/"
                    // + z + "/" + x + "/" + y
                    // +
                    // ".vector.pbf?access_token=pk.eyJ1IjoiZmljdGlvbmtpbmciLCJhIjoiY2s0OTkwdnZqMDFzMjNrbXczbWVrMmkzbyJ9.VZfDF5h7nHdfX58bCZM3yw";
                    // String urlstr = "http://demo.zjditu.cn/vtiles/tdt_zj/" + z + "/" + x + "/" +
                    // (y-8054) +".mvt?v=20180831"; //天地图服务器t0-t8间选一个
                    // String urlstr =
                    // "http://t0.tianditu.com/DataServer?T=vec_w&x="+x+"&y="+y+"&l="+z;
                    // //天地图服务器t0-t8间选一个
                    // String urlstr =
                    // "http://mt2.google.cn/vt/lyrs=m&scale=1&hl=zh-CN&gl=cn&x="+x+"&y="+y+"&z="+z;
                    // //谷歌地图服务器t0-t2间选一个
                    // String urlstr =
                    // "http://online3.map.bdimg.com/onlinelabel/?qt=tile&x="+x+"&y="+y+"&z="+z+"&&styles=pl&udt=20170712&scaler=1&p=1";
                    // //百度地图（加密过的）
                    // String urlstr = "http://c.tile.opencyclemap.org/cycle/" + z + "/" + x + "/" +
                    // y + ".png"; //osm地图
                    // String urlstr =
                    // "http://wprd04.is.autonavi.com/appmaptile?x="+x+"&y="+y+"&z="+z+"&lang=zh_cn&size=1&scl=1&style=8";
                    // //高德地图(6：影像，7：矢量，8：影像路网)
                    TileTask myTask = new TileTask();
                    myTask.tilepath = tilepath;
                    myTask.srcurl = srcurl;
                    myTask.filetype = filetype;
                    myTask.z = z;
                    myTask.x = x;
                    myTask.y = y;
                    myTask.semaphore = semaphore;

                    try {
                        semaphore.acquire();
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    myTask.start();

                    long per = (long) countdown * 10000 / (long) count;
                    printProgressBar(per, barlen);
                    System.out.print("[" + String.format("%02d", (threadsize - semaphore.availablePermits())) + "/"
                            + threadsize + "]");

                }
            }
            while (semaphore.availablePermits() < threadsize) {
                long per = (long) countdown * 10000 / (long) count;
                printProgressBar(per, barlen);
                System.out.print("[" + String.format("%02d", (threadsize - semaphore.availablePermits())) + "/"
                        + threadsize + "]");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            long per = (long) countdown * 10000 / (long) count;
            printProgressBar(per, barlen);
            System.out.print(
                    "[" + String.format("%02d", (threadsize - semaphore.availablePermits())) + "/" + threadsize + "]");
            System.out.println(" Completed!");

        }

    }

    public static class FontTask extends Thread {
        public String path;
        public String range;
        public URL url;
        public String stack;
        public Semaphore semaphore;

        @Override
        public void run() {
            try {
                File file = new File(path);
                if (file.exists()) {
                    increase();
                } else {
                    File tmpfile = new File(path + ".tmp");
                    String zxyStr = stack + "[" + range + "]";
                    Boolean ret;
                    ret = download(zxyStr, url, tmpfile);
                    if (ret) {
                        if (ProxyMap){
                            tmpfile.renameTo(file);
                            increase();
                        }else{
                            uncompress(tmpfile, file);
                            tmpfile.delete();
                            increase();
                        }
                    } else {
                        tmpfile.delete();
                    }
                }
            } catch (Exception e) {

            }

            semaphore.release();
        }
    }

    public static void downloadFont() throws IOException {
        System.out.println("开始下载字体...");
        int threadsize = 99;
        Semaphore semaphore = new Semaphore(threadsize);
        Iterator<String> iterator = glyphstack.keySet().iterator();
        int i = 0;
        while (iterator.hasNext()) {
            i++;
            String stack = iterator.next();
            String glyphurl = glyphstack.get(stack);
            System.out.println("[" + i + "/" + glyphstack.size() + "] " + stack);
            printProgressBar(0, barlen);
            countdown = 0;
            for (int j = 0; j <= 65535; j += 256) {
                int begin = j;
                int end = begin + 255;

                String urlstr = glyphurl.replace(PREFIX_MAPBOX + "fonts/", host+"/fonts/v1/");
                String codedstack = stack.replace(" ", "%20");
                String range = begin + "-" + end;
                urlstr = urlstr.replace("{fontstack}", codedstack);
                urlstr = urlstr.replace("{range}", range);
                URL url = getAccessUrl(urlstr);
                String path = storepath + "fonts/" + stack + "/" + range + ".pbf";

                FontTask myTask = new FontTask();
                myTask.path = path;
                myTask.range = range;
                myTask.stack = stack;
                myTask.url = url;
                myTask.semaphore = semaphore;

                try {
                    semaphore.acquire();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                myTask.start();
                long per = countdown * 256 * 10000 / 65536;
                printProgressBar(per, barlen);
                System.out.print("[" + String.format("%02d", (threadsize - semaphore.availablePermits())) + "/"
                        + threadsize + "]");

            }
            while (semaphore.availablePermits() < threadsize) {
                long per = countdown * 256 * 10000 / 65536;
                printProgressBar(per, barlen);
                System.out.print("[" + String.format("%02d", (threadsize - semaphore.availablePermits())) + "/"
                        + threadsize + "]");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            long per = countdown * 256 * 10000 / 65536;
            printProgressBar(per, barlen);
            System.out.print(
                    "[" + String.format("%02d", (threadsize - semaphore.availablePermits())) + "/" + threadsize + "]");
            System.out.println(" Completed!");

        }
    }

    private static Integer countdown;

    public static synchronized void increase() {
        countdown++;
    }

    /**
     * 远程文件下载
     *
     * @param url  下载地址
     * @param file 保存文件地址
     */
    public static boolean download(String zxystr, URL url, File file) throws IOException {
        boolean flag = true;
        DataOutputStream dos = null;
        DataInputStream dis = null;
        try {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (platform; rv:geckoversion) Gecko/geckotrail Firefox/firefoxversion");

            // 设置请求头，请求 GZIP 压缩的响应
            if (zxystr.contains("Tile:")){
                conn.setRequestProperty("Accept-Encoding", "gzip");
            }
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(20000);
            conn.connect();
            dos = new DataOutputStream(new FileOutputStream(file));
            dis = new DataInputStream(conn.getInputStream());
            byte[] data = new byte[2048];
            int i = 0;
            while ((i = dis.read(data)) != -1) {
                dos.write(data, 0, i);
            }
            dos.flush();
            // System.out.println(strDos);
        } catch (SocketTimeoutException e) {
            flag = false;
            errorMap.put(zxystr, e);
        } catch (IOException e) {
            flag = false;
            errorMap.put(zxystr, e);
        } catch (Exception e) {
            flag = false;
            errorMap.put(zxystr, e);
        } finally {
            if (dis != null)
                dis.close();
            if (dos != null)
                dos.close();
        }
        return flag;
    }

    /**
     * 计算分辨率
     *
     * @param maxLevel 最大级别
     */
    public static double[] getResolutions(int maxLevel) {
        double max = 360.0 / 256.0;
        double[] resolutions = new double[maxLevel + 1];
        for (int z = 0; z <= maxLevel; z++)
            resolutions[z] = max / Math.pow(2, z);
        return resolutions;
    }

    /**
     * 根据经度获取切片规范下的行号
     *
     * @param lon
     * @param zoom
     * @return
     */
    public static int getOSMTileXFromLongitude(double lon, int zoom) {
        return (int) (Math.floor((lon + 180) / 360 * Math.pow(2, zoom)));
    }

    /**
     * 根据纬度获取切片规范下的列号
     *
     * @param lat
     * @param zoom
     * @return
     */
    public static int getOSMTileYFromLatitude(double lat, int zoom) {
        return (int) (Math
                .floor((1 - Math.log(Math.tan(lat * Math.PI / 180) + 1 / Math.cos(lat * Math.PI / 180)) / Math.PI) / 2
                        * Math.pow(2, zoom)));
    }

    public static double mercatorXfromLng(double lng) {
        return (180 + lng) / 360;
    }

    public static double mercatorYfromLat(double lat) {
        return (180 - (180 / Math.PI * Math.log(Math.tan(Math.PI / 4 + lat * Math.PI / 360)))) / 360;
    }

    private static Map errorMap = new HashMap();

    public static boolean outerror() {
        boolean ret;
        System.out.println("===============================");
        if (errorMap.size() > 0) {
            System.out.println("这些下载失败了：");
            Iterator iterator = errorMap.keySet().iterator();
            while (iterator.hasNext()) {
                Object key = iterator.next();
                System.out.println(key + "：" + errorMap.get(key));
            }
            ret = false;
        } else {
            System.out.println("恭喜！全部下载成功！");
            ret = true;
        }
        System.out.println("===============================");
        return ret;
    }

    private static final int barlen = 30;

    public static void printProgressBar(long per, int len) {
        ConsoleOutHelper.home();
        String barstr = ConsoleOutHelper.progressbar((int) (per / 100), len);
        DecimalFormat df = new DecimalFormat("0.00");
        System.out.print(barstr + " " + df.format((double) per / 100) + "%");
    }

    public static String getTDTilesForArcGISPath(int x, int y, int z) {
        String l = "L" + String.format("%02d", z);
        String r = "R" + makePath(y);
        String c = "C" + makePath(x);
        String path = storepath + "tiles/" + l + File.separator + r + File.separator + c + ".png";
        return path;
    }

    public static String getTDTilesForCustomPath(int x, int y, int z) {
        String path = storepath + "tiles/" + z + File.separator + y + File.separator + x + ".png";
        return path;
    }

    public static String getTDTilesForCustomPathMvt(int x, int y, int z) {
        String path = storepath + "tilesMvt/" + z + File.separator + y + File.separator + x + ".mvt";
        return path;
    }

    public static String getOpenstreetForCustomPathMvt(int x, int y, int z) {
        String path = "tiles/" + z + File.separator + x + File.separator + y;
        return path;
    }

    private static String makePath(int num) {
        String str = Integer.toHexString(num);
        // ArcGIS行列都是8位长度
        while (str.length() < 8) {
            str = "0" + str;
        }
        return str;
    }

    public static void uncompress(File infile, File outfile) throws IOException {
        FileOutputStream dos = null;
        FileInputStream dis = null;
        GZIPInputStream ungzip = null;
        try {
            dos = new FileOutputStream(outfile);
            dis = new FileInputStream(infile);
            ungzip = new GZIPInputStream(dis);
            byte[] buffer = new byte[256];
            int n;
            if (ProxyMap){

            }
            while ((n = ungzip.read(buffer)) >= 0) {
                dos.write(buffer, 0, n);
            }
        } catch (IOException e) {
            System.out.println(e);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            if (dis != null)
                dis.close();
            if (dos != null)
                dos.close();
            if (ungzip != null)
                ungzip.close();
        }
    }

    public static String readFileContent(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuffer sbf = new StringBuffer();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return sbf.toString();
    }

    public static void writeFileContent(String fileName, String content) {
        File file = new File(fileName);
        FileWriter fw = null;
        try {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();
            fw = new FileWriter(file);
            fw.write(content);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fw != null)
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
    }

    public static String readJarFile(String filepath) {
        BufferedReader in = new BufferedReader(
                new InputStreamReader(MapDownload.class.getClassLoader().getResourceAsStream(filepath)));
        StringBuffer buffer = new StringBuffer();
        String line = "";
        try {
            while ((line = in.readLine()) != null) {
                buffer.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        String input = buffer.toString();
        return input;
    }

    public static void outputSamples() {
        String str = readJarFile("index.html");
        String replacement = "[[" + bound[0] + "," + bound[1] + "],[" + bound[2] + "," + bound[3] + "]]";
        str = str.replace("$bounds$", replacement);
        double center[] = { 0, 0 };
        center[0] = (bound[0] + bound[2]) / 2;
        center[1] = (bound[1] + bound[3]) / 2;
        replacement = "[" + center[0] + "," + center[1] + "]";
        str = str.replace("$center$", replacement);
        replacement = zoom[0] + "";
        str = str.replace("$zoom$", replacement);
        str = str.replace("$minZoom$", replacement);
        replacement = zoom[1] + "";
        str = str.replace("$maxZoom$", replacement);
        writeFileContent(storepath + "index.html", str);

        str = readJarFile("mapbox-gl.js");
        writeFileContent(storepath + "mapbox-gl.js", str);

        str = readJarFile("mapbox-gl.css");
        writeFileContent(storepath + "mapbox-gl.css", str);

        str = readJarFile("mapbox-gl-language.js");
        writeFileContent(storepath + "mapbox-gl-language.js", str);

        str = readJarFile("mapbox-gl-rtl-text.js");
        writeFileContent(storepath + "mapbox-gl-rtl-text.js", str);
    }
}
