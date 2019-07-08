package com.rengu.project.integrationoperations.service;

import com.rengu.project.integrationoperations.entity.*;
import com.rengu.project.integrationoperations.enums.SystemStatusCodeEnum;
import com.rengu.project.integrationoperations.exception.SystemException;
import com.rengu.project.integrationoperations.repository.CMDSerialNumberRepository;
import com.rengu.project.integrationoperations.repository.HostRepository;
import com.rengu.project.integrationoperations.thread.TCPThread;
import com.rengu.project.integrationoperations.util.SocketConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.rengu.project.integrationoperations.util.SocketConfig.BinaryToDecimal;

/**
 * @Author: yaojiahao
 * @Date: 2019/4/12 13:32
 */
@Service
@Slf4j
public class DeploymentService {
    private byte backups = 0;
    private final CMDSerialNumberRepository cmdSerialNumberRepository;
    private short shorts = 0;
    private final HostRepository hostRepository;
    private Set<String> set = new HashSet<>();

    @Autowired
    public DeploymentService(CMDSerialNumberRepository cmdSerialNumberRepository, HostRepository hostRepository) {
        this.cmdSerialNumberRepository = cmdSerialNumberRepository;
        this.hostRepository = hostRepository;
    }


    //  系统控制指令帧格式说明（头部固定信息）
    private void sendSystemControlCmdFormat(ByteBuffer byteBuffer) {
        // 报文头 7E9118E7
        byteBuffer.putInt(2122389735);
        // 当前包数据长度
    }

    //  发送时间
    public void sendSystemTiming(String time, String host) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(40);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            sendSystemControlCmdFormat(byteBuffer);
            byteBuffer.putInt(1); // 当前包数据长度
            byteBuffer.putShort(shorts);  // 目的地址(设备ID号)
            byteBuffer.putShort(shorts);  // 源地址(设备ID号)
            byteBuffer.put(backups); // 域ID(预留)
            byteBuffer.put(backups); // 主题ID(预留)
            byteBuffer.putShort(shorts);  // 信息类别号 (各种交换的信息格式报分配唯一的编号)
            byteBuffer.putLong(1);  // 发报日期时间
            byteBuffer.putInt(1);  // 序列号 (同批数据的序列号相同，不同批数据的序列号不同)
            byteBuffer.putInt(1);  // 包总数 (当前发送的数据，总共分成几个包发送。默认一包)
            byteBuffer.putInt(1);  // 当前包号 (当前发送的数据包序号。从1开始，当序列号不同时，当前包号清零，从1开始。)
            byteBuffer.putInt(1);  // 数据总长度
            byteBuffer.putShort(shorts); // 版本号
            byteBuffer.putInt(0); // 保留字段
            byteBuffer.putShort(shorts); // 保留字段

            /* byteBuffer.putInt(addSerialNum());
            byteBuffer.putInt(40);
            //  指令类型
            byteBuffer.putShort((short) 1);
            byteBuffer.putShort((short) 1);*/
            //  包头
            byteBuffer.putShort(SocketConfig.header);
            //  解时间
            if (time.isEmpty()) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:SSS:dd:MM:yyyy");
                time = simpleDateFormat.format(new Date());
            }
            byte hour = Byte.parseByte(time.substring(0, 2));
            byteBuffer.put(hour);
            byte minute = Byte.parseByte(time.substring(3, 5));
            byteBuffer.put(minute);
            //  秒>毫秒>int>16进制
            byteBuffer.putShort(Short.parseShort(time.substring(6, 9)));
//            String millisecond = Integer.toHexString(Integer.parseInt(time.substring(6, 9)) );
//            byte[] byteMS = SocketConfig.hexToByte(millisecond);
//            byteBuffer.put(byteMS);
//            if (byteMS.length == 0) {
//                byteBuffer.putShort(shorts);
//            }
            byte day = Byte.parseByte(time.substring(10, 12));
            byteBuffer.put(day);
            byte month = Byte.parseByte(time.substring(13, 15));
            byteBuffer.put(month);
            short year = Short.parseShort(time.substring(16));
            byteBuffer.putShort(year);
            //  包尾
            getPackageTheTail(byteBuffer);

            byteBuffer.putInt(0); // 校验和 (暂时预留)

            /*int a = getByteCount(byteBuffer);
            byteBuffer.putInt(a);*/
            //  帧尾
            getBigPackageTheTail(byteBuffer);
            OutputStream outputStream = null;
            Socket socket = (Socket) TCPThread.map.get(host);
            if (TCPThread.map.get(host) != null) {
                outputStream = socket.getOutputStream();
            }
            assert outputStream != null;
            outputStream.write(byteBuffer.array());
        } catch (IOException e) {
            throw new SystemException(SystemStatusCodeEnum.SOCKET_CONNENT_ERROR);
        }
    }

    //  分机控制指令
    public String sendExtensionControlCMD(ExtensionControlCMD extensionControlCMD, String host) throws SystemException {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(60);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            //  包头
            sendSystemControlCmdFormat(byteBuffer);
            byteBuffer.putInt(1); // 当前包数据长度
            byteBuffer.putShort(shorts);  // 目的地址(设备ID号)
            byteBuffer.putShort(shorts);  // 源地址(设备ID号)
            byteBuffer.put(backups); // 域ID(预留)
            byteBuffer.put(backups); // 主题ID(预留)
            byteBuffer.putShort(shorts);  // 信息类别号 (各种交换的信息格式报分配唯一的编号)
            byteBuffer.putLong(2);  // 发报日期时间
            byteBuffer.putInt(2);  // 序列号 (同批数据的序列号相同，不同批数据的序列号不同)
            byteBuffer.putInt(2);  // 包总数 (当前发送的数据，总共分成几个包发送。默认一包)
            byteBuffer.putInt(2);  // 当前包号 (当前发送的数据包序号。从1开始，当序列号不同时，当前包号清零，从1开始。)
            byteBuffer.putInt(2);  // 数据总长度
            byteBuffer.putShort(shorts); // 版本号
            byteBuffer.putInt(0); // 保留字段
            byteBuffer.putShort(shorts); // 保留字段
            byteBuffer.putShort(SocketConfig.header);
            byte pulse = Byte.parseByte(extensionControlCMD.getPulse());
            byteBuffer.put(pulse);
            byteBuffer.put(backups);
            //  分机控制字
            extensionControl(extensionControlCMD.getExtensionControlCharacter(), byteBuffer);
            byteBuffer.put(backups);
            byteBuffer.put(Byte.parseByte(extensionControlCMD.getThreshold()));
            byteBuffer.putShort((Short.parseShort(extensionControlCMD.getOverallpulse())));
            byteBuffer.put(Byte.parseByte(extensionControlCMD.getMinamplitude()));
            byte c = (byte) (Integer.parseInt(extensionControlCMD.getMaxamplitude()) & 0xff);
            byteBuffer.put(c);
            byteBuffer.putShort(Short.parseShort(extensionControlCMD.getMinPulsewidth()));
            byteBuffer.putShort(Short.parseShort(extensionControlCMD.getMaxPulsewidth()));
            byteBuffer.putShort(Short.parseShort(extensionControlCMD.getFilterMaximumFrequency()));
            byteBuffer.putShort(Short.parseShort(extensionControlCMD.getFilterMinimumFrequency()));
            byteBuffer.putShort(Short.parseShort(extensionControlCMD.getShieldingMaximumFrequency()));
            byteBuffer.putShort(Short.parseShort(extensionControlCMD.getShieldingMinimumFrequency()));
            //  默认值更新标记
            if (extensionControlCMD.getDefalutUpdate().equals("0")) {
                byteBuffer.put((byte) 0);
            } else if (extensionControlCMD.getDefalutUpdate().equals("1")) {
                byteBuffer.put((byte) 1);
            }

            byteBuffer.putShort(shorts);
            byteBuffer.put(backups);
            byteBuffer.putShort(shorts);
            //  包尾
            getPackageTheTail(byteBuffer);

            byteBuffer.putInt(0); // 校验和 (暂时预留)
            /*int a = getByteCount(byteBuffer);
            byteBuffer.putInt(a);*/
            getBigPackageTheTail(byteBuffer);  //  帧尾
            OutputStream outputStream = null;
            Socket socket = (Socket) TCPThread.map.get(host);
            if (TCPThread.map.get(host) != null) {
                outputStream = socket.getOutputStream();
            }
            assert outputStream != null;
            outputStream.write(byteBuffer.array());
            return "SUCCESS";
        } catch (IOException e) {
            throw new SystemException(SystemStatusCodeEnum.SOCKET_CONNENT_ERROR);
        }
    }

    //  分机控制字(解析String为bit)
    private void extensionControl(String eccs, ByteBuffer byteBuffer) {
        //  截取分机控制字中的数据》再将二进制转换成十进制
        StringBuilder stringBuilders = new StringBuilder();
        stringBuilders.append(eccs);
        String ecc = stringBuilders.reverse().toString();
        StringBuilder stringBuilder = new StringBuilder();
        //  合路选择
        String mergeToChoose = ecc.substring(0, 1);
        switch (mergeToChoose) {
            case "2":
                stringBuilder.append("10");
                break;
            case "1":
                stringBuilder.append("01");
                break;
            case "0":
                stringBuilder.append("00");
                break;
            default:
                throw new SystemException(SystemStatusCodeEnum.ExtensionControlCharacter_ERROR);
        }
        //  校正
        String revise = ecc.substring(1, 2);
        switch (revise) {
            case "3":
                stringBuilder.append("11");
                break;
            case "2":
                stringBuilder.append("10");
                break;
            case "1":
                stringBuilder.append("01");
                break;
            case "0":
                stringBuilder.append("00");
                break;
            default:
                throw new SystemException(SystemStatusCodeEnum.ExtensionControlCharacter_ERROR);
        }
        //  校准模式选择
        stringBuilder.append(ecc, 2, 3);
        //  分选预处理模式
        String pretreatment = ecc.substring(3, 4);
        if (pretreatment.equals("1")) {
            stringBuilder.append("01");
        } else if (pretreatment.equals("0")) {
            stringBuilder.append("00");
        } else {
            throw new SystemException(SystemStatusCodeEnum.ExtensionControlCharacter_ERROR);
        }
        //  脉冲
        String allPulse = ecc.substring(4, 5);
        switch (allPulse) {
            case "3":
                stringBuilder.append("011");
                break;
            case "2":
                stringBuilder.append("010");
                break;
            case "1":
                stringBuilder.append("001");
                break;
            case "0":
                stringBuilder.append("000");
                break;
            default:
                throw new SystemException(SystemStatusCodeEnum.ExtensionControlCharacter_ERROR);
        }
        stringBuilder.append(ecc.substring(5));
        byte[] bytes = new byte[2];
        String reverse = stringBuilder.toString();
        bytes[0] = (byte) BinaryToDecimal(Integer.parseInt(reverse.substring(0, 5)));
        bytes[1] = (byte) BinaryToDecimal(Integer.parseInt(reverse.substring(5)));
        byteBuffer.put(bytes[0]);
        byteBuffer.put(bytes[1]);
    }

    //  发送系统控制指令
    public String sendSystemControlCMD(SystemControlCMD systemControlCMD, String host) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(76);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            //  包头
            sendSystemControlCmdFormat(byteBuffer);
            byteBuffer.putInt(1); // 当前包数据长度
            byteBuffer.putShort(shorts);  // 目的地址(设备ID号)
            byteBuffer.putShort(shorts);  // 源地址(设备ID号)
            byteBuffer.put(backups); // 域ID(预留)
            byteBuffer.put(backups); // 主题ID(预留)
            byteBuffer.putShort(shorts);  // 信息类别号 (各种交换的信息格式报分配唯一的编号)
            byteBuffer.putLong(3);  // 发报日期时间
            byteBuffer.putInt(3);  // 序列号 (同批数据的序列号相同，不同批数据的序列号不同)
            byteBuffer.putInt(3);  // 包总数 (当前发送的数据，总共分成几个包发送。默认一包)
            byteBuffer.putInt(3);  // 当前包号 (当前发送的数据包序号。从1开始，当序列号不同时，当前包号清零，从1开始。)
            byteBuffer.putInt(3);  // 数据总长度
            byteBuffer.putShort(shorts); // 版本号
            byteBuffer.putInt(0); // 保留字段
            byteBuffer.putShort(shorts); // 保留字段
            byteBuffer.putShort(SocketConfig.header);
            systemControlCMD.getWorkPattern();
            byteBuffer.put(Byte.parseByte(systemControlCMD.getWorkPattern()));
            byteBuffer.put(Byte.parseByte(systemControlCMD.getWorkCycle()));
            byteBuffer.putShort(Short.parseShort(systemControlCMD.getWorkCycleAmount()));
            byteBuffer.putShort(Short.parseShort(systemControlCMD.getBeginFrequency()));
            byteBuffer.putShort(Short.parseShort(systemControlCMD.getEndFrequency()));
            byteBuffer.putShort(Short.parseShort(systemControlCMD.getSteppedFrequency()));
            byteBuffer.put(Byte.parseByte(systemControlCMD.getSteppedFrequency()));
            byteBuffer.putShort(shorts);
            byteBuffer.put(backups);
            byteBuffer.put(Byte.parseByte(systemControlCMD.getChooseAntenna1()));
            byteBuffer.put(Byte.parseByte(systemControlCMD.getChooseAntenna2()));
            byteBuffer.putShort(shorts);
            //  射频一衰减(最新)
           /* StringBuilder stringBuilder = new StringBuilder();
            String attenuationRF1 = stringBuilder.reverse().append(systemControlCMD.getAttenuationRF1()).toString();
            byte bytes = (byte) BinaryToDecimal(Integer.parseInt(attenuationRF1));*/
            byte b=0;
            byteBuffer.put(b);

            //  射频一长电缆均衡衰减控制
//            StringBuilder stringBuilders = new StringBuilder();
            //  反转数组的原因是因为二级制从第0位开始是从右边开始的，而传过来的值第0位在最左边，所以需要反转
//            String balancedAttenuationRF1 = stringBuilders.reverse().append(systemControlCMD.getBalancedAttenuationRF1()).toString();
//            byte bytesAttenuationRF1 = (byte) SocketConfig.BinaryToDecimal(Integer.parseInt(balancedAttenuationRF1));

            /*(最新)
            byte bytesAttenuationRF1 = (byte) BinaryToDecimal(Integer.parseInt(systemControlCMD.getBalancedAttenuationRF1()));
            byteBuffer.put(bytesAttenuationRF1);*/
            byteBuffer.put(b);

            byteBuffer.putShort(shorts);
            //  射频二控制衰减
//            StringBuilder stringBuilder2 = new StringBuilder();
//            String attenuationRF2 = stringBuilder2.reverse().append(systemControlCMD.getAttenuationRF2()).toString();
//            byte byteAttenuationRF2 = (byte) SocketConfig.BinaryToDecimal(Integer.parseInt(attenuationRF2));

            /*(最新)
            byte byteAttenuationRF2 = (byte) BinaryToDecimal(Integer.parseInt(systemControlCMD.getBalancedAttenuationRF2()));
            byteBuffer.put(byteAttenuationRF2);*/
            byteBuffer.put(b);

            /*射频二长电缆均衡衰减控制(最新)
            StringBuilder stringBuilderAttenuationRF2 = new StringBuilder();
            String balancedAttenuationRF2 = stringBuilderAttenuationRF2.reverse().append(systemControlCMD.getBalancedAttenuationRF2()).toString();
            byte bytesAttenuationRF2 = (byte) BinaryToDecimal(Integer.parseInt(balancedAttenuationRF2));
            byteBuffer.put(bytesAttenuationRF2);*/
            byteBuffer.put(b);

            byteBuffer.putShort(shorts);
           /* 中频一衰减(最新)
            byte bytesAttenuationMF1 = (byte) BinaryToDecimal(Integer.parseInt(systemControlCMD.getAttenuationMF1()));
            byteBuffer.put(bytesAttenuationMF1);*/
            byteBuffer.put(b);

            byteBuffer.putShort(shorts);
            byteBuffer.put(backups);
            /*中频二衰减(最新)
            byte bytesAttenuationMF2 = (byte) BinaryToDecimal(Integer.parseInt(systemControlCMD.getAttenuationMF2()));
            byteBuffer.put(bytesAttenuationMF2);*/
            byteBuffer.put(b);

//            byteBuffer.put(Byte.parseByte(systemControlCMD.getAttenuationControlWay()));
            byteBuffer.put(b);

            byteBuffer.putShort(shorts);
            //  自检源衰减
            byteBuffer.put(Byte.parseByte(systemControlCMD.getSelfInspectionAttenuation()));
            //  脉内引导批次开关
            byteBuffer.put(Byte.parseByte(systemControlCMD.getGuidanceSwitch()));
            //  脉内引导批次号
            byteBuffer.put(Byte.parseByte(systemControlCMD.getGuidance()));
            //  故障检测门限
            byteBuffer.put(Byte.parseByte(systemControlCMD.getFaultDetect()));

            //  定时时间码

       /*     String time = systemControlCMD.getTimingCode();
            //  转换2进制
            StringBuilder month = new StringBuilder(Integer.toBinaryString(Integer.parseInt(time.substring(0, 2))));
            StringBuilder day = new StringBuilder(Integer.toBinaryString(Integer.parseInt(time.substring(2, 4))));
            StringBuilder hour = new StringBuilder(Integer.toBinaryString(Integer.parseInt(time.substring(4, 6))));
            StringBuilder minute = new StringBuilder(Integer.toBinaryString(Integer.parseInt(time.substring(6, 8))));
            StringBuilder second = new StringBuilder(Integer.toBinaryString(Integer.parseInt(time.substring(8, 10))));
            //  拼接秒数
            int seconds = second.length();
            for (int i = 0; i < 11 - seconds; i++) {
                second.insert(0, "0");
            }
            //  拼接分钟
            int minutes = minute.length();
            for (int i = 0; i < 6 - minutes; i++) {
                minute.insert(0, "0");
            }
            //  拼接时钟
            int hours = hour.length();
            for (int i = 0; i < 5 - hours; i++) {
                hour.insert(0, "0");
            }
            //  拼接天数
            int days = day.length();
            for (int i = 0; i < 5 - days; i++) {
                day.insert(0, "0");
            }
            //  拼接月份
            int months = month.length();
            for (int i = 0; i < 4 - months; i++) {
                month.insert(0, "0");
            }
            String thisTime = month.toString() + day.toString() + hour.toString() + minute.toString() + second.toString();
            byte[] bytes1 = new byte[4];
            bytes1[0] = (byte) BinaryToDecimal(Integer.parseInt(thisTime.substring(0, 8)));
            bytes1[1] = (byte) BinaryToDecimal(Integer.parseInt(thisTime.substring(8, 16)));
            bytes1[2] = (byte) BinaryToDecimal(Integer.parseInt(thisTime.substring(16, 24)));
            bytes1[3] = (byte) BinaryToDecimal(Integer.parseInt(thisTime.substring(24)));
            for (byte c : bytes1) {
                byteBuffer.put(c);
            }*/
            int d=0;
            byteBuffer.putInt(d);

            //  单次执行指令集所需时间
            byteBuffer.putShort(shorts);
            getPackageTheTail(byteBuffer);
            byteBuffer.putInt(0); // 校验和 (暂时预留)
            /*int a = getByteCount(byteBuffer);
            byteBuffer.putInt(a);*/
            getBigPackageTheTail(byteBuffer);  //  帧尾
            OutputStream outputStream = null;
            Socket socket = (Socket) TCPThread.map.get(host);
            if (TCPThread.map.get(host) != null) {
                outputStream = socket.getOutputStream();
            }
            assert outputStream != null;
            outputStream.write(byteBuffer.array());
            return "SUCCESS";
        } catch (IOException e) {
            throw new SystemException(SystemStatusCodeEnum.SOCKET_CONNENT_ERROR);
        }
    }

    //  群发系统控制指令
    public void sendAllSystemControlCMD(SystemControlCMD systemControlCMD) {
        List<AllHost> list = hostRepository.findAll();
        for (AllHost allHost : list) {
            sendSystemControlCMD(systemControlCMD, allHost.getHost());
        }
    }

    //  群发分机控制指令
    public void sendAllExtensionControl(ExtensionControlCMD extensionControlCMD) {
        List<AllHost> list = hostRepository.findAll();
        for (AllHost allHost : list) {
            sendExtensionControlCMD(extensionControlCMD, allHost.getHost());
        }
    }

    //  群发系统校时
    public void sendAllSendSystemTiming(String time) throws IOException {
        List<AllHost> list = hostRepository.findAll();
        for (AllHost allHost : list) {
            sendSystemTiming(time, allHost.getHost());
        }
    }

    //  封装包尾信息
    private void getPackageTheTail(ByteBuffer byteBuffer) {
        byte[] bytes = SocketConfig.hexToByte(SocketConfig.end);
        byteBuffer.put(bytes);
    }

    //  封装大包尾信息
    private void getBigPackageTheTail(ByteBuffer byteBuffer) {
/*        String frameEnd = "55aa55aa";
        byte[] bytes = SocketConfig.hexToByte(frameEnd);*/
        byteBuffer.putInt(150536440);
    }

    // 查询当前编号,并且+1
    private int addSerialNum() {
        List<CMDSerialNumber> list = cmdSerialNumberRepository.findAll();
        int a = 0;
        if (list.size() == 1) {
            for (CMDSerialNumber cmdSerialNumber : list) {
                assert cmdSerialNumber != null;
                a = cmdSerialNumber.getSerialNumber() + 1;
                cmdSerialNumber.setSerialNumber(a);
                cmdSerialNumberRepository.save(cmdSerialNumber);
            }
        }
        return a;
    }

    // 在校验和的所有字节数相加
    private int getByteCount(ByteBuffer byteBuffer) {
        byte[] b = byteBuffer.array();
        int c = 0;
        for (byte a : b) {
            String s = Integer.toBinaryString((a & 0xFF) + 0x100).substring(1);
            c += BinaryToDecimal(Integer.parseInt(s));

        }
        return c;
    }



}
