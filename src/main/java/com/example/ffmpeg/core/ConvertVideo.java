package com.example.ffmpeg.core;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.listener.PageReadListener;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class ConvertVideo {

    private static String PATH = "D:\\cut\\";
    private final static String ffmpegPath = "D:\\cut\\ffmpeg-master\\bin\\ffmpeg.exe";
    private final static String excelPath = "D:\\cut\\1.xlsx";

    public static void main(String[] args) {
        Scanner input=new Scanner(System.in);
        String str=input.next();
        PATH = PATH+str+".flv";
        if (!checkfile(PATH)) {
            System.out.println(PATH + " is not file");
            return;
        }
        List timeList = readExcel();
        if (process(timeList)) {
            System.out.println("ok");
        }
    }

    private static boolean process(List<DemoData> timeList) {
        int type = checkContentType();
        boolean status = false;
        if (type == 0) {
            System.out.println("直接将文件转为flv文件");
            SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");
            String videoOutPath = "D:/cut/"+sdf.format(new Date())+"/";
            log.info("开始剪切视频");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i <timeList.size() ; i++) {
                try {
                    status = videoClip(PATH,videoOutPath,timeList.get(i).getStartTime(),timeList.get(i).getEndTime(),timeList.get(i).getId());// 直接将文件转为flv文件
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            long endTime = System.currentTimeMillis();
            log.info("剪切花费时间"+(endTime-startTime)/1000+"秒");
            log.info("开始生成filelist");
            fileList(videoOutPath,timeList);
            log.info("开始合并视频");
            startTime = System.currentTimeMillis();
            try {
                videoMerger(videoOutPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            endTime = System.currentTimeMillis();
            log.info("合并花费时间"+(endTime-startTime)/1000+"秒");
        }
        return status;
    }

    private static void fileList(String videoOutputPath,List<DemoData> timeList) {
        StringBuffer videoInputString = new StringBuffer();
        for (DemoData time : timeList) {
            videoInputString.append("file");
            videoInputString.append(" ");
            videoInputString.append("\'");
            videoInputString.append(time.getId()+".flv");
            videoInputString.append("\'");
            videoInputString.append("\r\n");
        }
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter(videoOutputPath+"fileList.txt"));
            out.write(videoInputString.toString());
            out.close();
            System.out.println("文件创建成功！");
        } catch (IOException e) {
        }
    }

    private static List readExcel() {
        List<DemoData> timeList = new ArrayList<>();
        EasyExcel.read(excelPath, DemoData.class, new PageReadListener<DemoData>(timeList::addAll)).sheet().doRead();
        return timeList;
    }

    private static int checkContentType() {
        String type = PATH.substring(PATH.lastIndexOf(".") + 1, PATH.length())
                .toLowerCase();
        // ffmpeg能解析的格式：（asx，asf，mpg，wmv，3gp，mp4，mov，avi，flv等）
        if (type.equals("avi")) {
            return 0;
        } else if (type.equals("mpg")) {
            return 0;
        } else if (type.equals("wmv")) {
            return 0;
        } else if (type.equals("3gp")) {
            return 0;
        } else if (type.equals("mov")) {
            return 0;
        } else if (type.equals("mp4")) {
            return 0;
        } else if (type.equals("asf")) {
            return 0;
        } else if (type.equals("asx")) {
            return 0;
        } else if (type.equals("flv")) {
            return 0;
        }
        // 对ffmpeg无法解析的文件格式(wmv9，rm，rmvb等),
        // 可以先用别的工具（mencoder）转换为avi(ffmpeg能解析的)格式.
        else if (type.equals("wmv9")) {
            return 1;
        } else if (type.equals("rm")) {
            return 1;
        } else if (type.equals("rmvb")) {
            return 1;
        }
        return 9;
    }

    private static boolean checkfile(String path) {
        File file = new File(path);
        if (!file.isFile()) {
            return false;
        }
        return true;
    }

    /**
     *
     *剪切视频
     videoInputPath 需要处理的视频路径
     startTime： 截取的开始时间 格式为 00:00:00（时分秒）
     endTime: 截取的结束时间 格式为00:03:00（时分秒）
     devIp： 通道号  业务存在 ，可自行删除
     * */

    public static boolean videoClip(String videoInputPath,String videoOutputPath,String startTime,String endTime,String id) throws IOException {


        File file = new File(videoOutputPath);
        if (!file.exists()){
            file.mkdirs();
        }
        videoOutputPath = videoOutputPath+id+".flv";
        List<String> command = new ArrayList<String>();
        command.add(ffmpegPath);
        command.add("-ss");
        command.add(startTime);
        command.add("-to");
        command.add(endTime);
        command.add("-i");
        command.add(videoInputPath);
        command.add("-vcodec");
        command.add("copy");
        command.add("-acodec");
        command.add("copy");
        command.add(videoOutputPath);
        command.add("-y");
        ProcessBuilder builder = new ProcessBuilder(command);
        System.out.println(command.toString());
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        InputStream errorStream = process.getErrorStream();
        InputStreamReader inputStreamReader = new InputStreamReader(errorStream);
        BufferedReader br = new BufferedReader(inputStreamReader);
        String line = "";
        while ((line = br.readLine()) != null) {
        }
        if (br != null) {
            br.close();
        }
        if (inputStreamReader != null) {
            inputStreamReader.close();
        }
        if (errorStream != null) {
            errorStream.close();
        }

        return true;
    }

    public static boolean videoMerger(String videoOutPath) throws IOException {



        SimpleDateFormat sdf=new SimpleDateFormat("yyyyMMdd");

        List<String> command = new ArrayList<String>();
        command.add(ffmpegPath);
        command.add("-f");
        command.add("concat");
        command.add("-i");
        command.add(videoOutPath+"filelist.txt");
        command.add("-c");
        command.add("copy");
        command.add(videoOutPath+sdf.format(new Date())+".flv");
        command.add("-y");
        ProcessBuilder builder = new ProcessBuilder(command);
        System.out.println(command);
        Process process = null;
        try {
            process = builder.start();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        InputStream errorStream = process.getErrorStream();
        InputStreamReader inputStreamReader = new InputStreamReader(errorStream);
        BufferedReader br = new BufferedReader(inputStreamReader);
        String line = "";
        while ((line = br.readLine()) != null) {
        }
        if (br != null) {
            br.close();
        }
        if (inputStreamReader != null) {
            inputStreamReader.close();
        }
        if (errorStream != null) {
            errorStream.close();
        }

        return true;
    }
}