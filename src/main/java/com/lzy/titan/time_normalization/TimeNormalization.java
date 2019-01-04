package com.lzy.titan.time_normalization;

import static org.junit.Assert.assertEquals;

/**
 * 时间索引编码
 * 
 * 解决问题：
 *    在数据仓库（hadoop->hive etc.）表和表做业务join，连接字段就比较复杂,如果需要对相似时间delta T
 *    时间范围内做时间方向上统一，不好做，基于Mapreduce是可以的，对reduce line by line去过时间片就可以搞定，但是
 *    对于hive等数据仓库，基于sql，需要join字段，但是 空间 时间等，难以做相似位置，或者相似时间的统一和归一化。
 *    
 * 算法参考：geohash 对空间做的索引编码
 * 
 * 此项目实现：对时间的空间索引和归一化 可以将相似时间  如时间间隔 3s/50s/3min/...等时间跨度范围做数值上的统一。
 * 由于时间是不收敛的，不像longitude或者latitude有限制，所以我们采用100年的这样的限制方案，二进制编码的范围采用1970~2070年timestamp的方式。
 * 
 * 
 * algorithm is refereneced by https://en.wikipedia.org/wiki/Geohash
 * 
 * @author kongxianghe
 * @since 2019-01-03
 *
 */
public class TimeNormalization {
	// max timestamp number
	public static final Long MAX_TIMESTAMP = 3155731200l; // 2070-01-01 00:00:00  (timestamp)
	public static final Long MIN_TIMESTAMP = 0l;   // 1970-01-01 00:00:00  (timestamp)
	// bit number to split timestamp,binary split
	public static final Integer BIT_NUMBER_32 = 32;
	public static final Integer MAX_PRECISION_16 = 16;
	
	// 32bit每一位 *2  最低位代表的是0.73秒
	public static final Double BASE_BIT_NUM = MAX_TIMESTAMP/Math.pow(2, BIT_NUMBER_32);
	
	
	/**
	 * 正规化
	 * 
	 * 
	 * 	取后N位不一致
		3s  ->  后2bit  2^2*0.734   --> 15位相同
		12s ->  后4bit  2^4*0.734   --> 14位相同
		50s ->  后6bit 2^6*0.734   --> 13位相同
		3min -> 后8bit  2^8*0.734   --> 12位相同
		12min -> 后10bit 2^10*0.734 --> 11位相同
		50min -> 后12bit 2^12*0.734 --> 10位相同
		200min -> 后14bit 2^14*0.734 --> 9位相同
		13.3h -> 后16bit 2^16*0.734 --> 8位相同

		base4编码，每2bit编码一次 00 01 10 11 一共采用Base4 0 1 2 3 三个数字，一共16位数字
	 * 
	 * @param timestamp 到秒的时间戳
	 * @return
	 */
	public String normalize(Long timestamp){
		String retval = "";
		if(timestamp < 0){
			throw new IllegalArgumentException("timestamp value must be positive!");
		}
		byte[] bitArr = timeToBit(timestamp);
		retval = encodeBase4(bitArr);
		return retval;
	}
	
	/**
	 * 正规化数值按照精度返回
	 * 
	 * @param timestamp
	 * @param precision
	 * @return
	 */
	public String normalize(Long timestamp,int precision){
		String retval = "";
		if (precision > MAX_PRECISION_16 || precision <= 0) {
			throw new IllegalArgumentException("timehash string illegal,must in [0,16]");
		}
		retval = normalize(timestamp).substring(0, precision);
		return retval;
	}
	
	/**
	 * 通过timehash值 反推时间戳
	 * @param timeHash
	 * @return timestamp
	 */
	public Long deNormalize(String timeHash){
		if(timeHash==null || timeHash.length()!= MAX_PRECISION_16){
			throw new IllegalArgumentException("deNormalize use timehash string illegal,length must be 16");
		}
		
		return bitToTime(decodeBase4(timeHash));
	}
	
	public String encodeBase4(byte[] bitArr) {
		if(bitArr == null || bitArr.length%2 != 0){
			throw new IllegalArgumentException("must be dual.");
		}
		StringBuilder builder = new StringBuilder();
		int stepSize = 2;	// base4:means 2^2  2bit
		for(int i = 0;i < bitArr.length;i += stepSize){
			int tempCode = (bitArr[i]<<1) + bitArr[i+1];
			builder.append(tempCode);
		}
		return builder.toString();
	}
	
	public byte[] decodeBase4(String base4Str){
		if(base4Str==null || base4Str == ""){
			return null;
		}
		int arrLen = base4Str.length()*2;
		byte[] retval = new byte[arrLen];
		
		for(int i = 0;i < base4Str.length();i++){
			String eachChar = base4Str.substring(i, i+1);
			String eachCode = Integer.toBinaryString(Integer.parseInt(eachChar));
			if(eachCode.length()>2){
				throw new IllegalArgumentException("base4 code must in 0 1 2 3.");
			}
			if(eachCode.length()==1){
				retval[i*2]=0;
				retval[i*2+1]=Byte.parseByte(eachCode);
			}else if(eachCode.length()==2){
				String highChar = eachCode.substring(0, 1);
				String lowChar = eachCode.substring(1, 2);
				retval[i*2]=Byte.parseByte(highChar);
				retval[i*2+1]=Byte.parseByte(lowChar);
			}else {
				// do nothing
			}
		}
		return retval;
	}
	

	/**
	 * 3155731200000  -> 2070-01-01 00:00:00 timestamp
	 * 0              -> 1970-01-01 00:00:00 timestamp
	 * 把这个数字做2^32  做二分，做二进制编码，时间范围(0~3155731200000)
	 * 
	 * 3155731200000/2^32 =~ 0.73s基本上满足业务使用
	 * 
	 * [min,mid) 二进制编码取 0
	 * [mid,max] 二进制编码取 1
	 * 
	 */
	public byte[] timeToBit(Long timestamp){
		byte[] retval = new byte[BIT_NUMBER_32];
		
		double minVal = MIN_TIMESTAMP.doubleValue();
		double maxVal = MAX_TIMESTAMP.doubleValue();
		
		
		for(int i = 0;i < BIT_NUMBER_32;i++){
			double tmpTime = minVal+maxVal;
			tmpTime = tmpTime/2d;
			// 中间值和当前时间戳的比较
			int compFlag = Double.compare(timestamp.doubleValue(), tmpTime);
			// 取 1
			if(compFlag>=0){
				minVal = tmpTime;
				retval[i] = 1;
			}
			// 取 0
			else{
				maxVal = tmpTime;
				retval[i] = 0;
			}
		}
		return retval;
	}
	
	/**
	 * must be in 32bit bytes array
	 * @param bits
	 * @return
	 */
	public Long bitToTime(byte[] bits){
		double retval = 0l;
		for(int i = 0;i < BIT_NUMBER_32;i++){
			double a = (bits[i] & 0xff) *BASE_BIT_NUM* Math.pow(2, BIT_NUMBER_32-i-1);
			retval+=a;
		}
		return Math.round(retval);
	}
	
	
	public String leftFill(String str,int length,char fillChar){
		String oriStr = str;
		String retval = "";
		if(str == null || str == ""){
			oriStr = "";
		}
		
		for(int i=0;i<length-oriStr.length();i++){
			retval+=fillChar;
		}
		retval+= oriStr;
		return retval;
	}
	
	public String rightFill(String str,int length,char fillChar){
		String oriStr = str;
		String retval = "";
		if(str == null || str == ""){
			oriStr = "";
		}
		retval = oriStr;
		for(int i=oriStr.length();i<length;i++){
			retval+=fillChar;
		}
		return retval;
	}
	
	public String intArrayToString(byte[] objs){
		StringBuilder builder = new StringBuilder();
		for(Object obj:objs){
			builder.append(obj);
		}
		//System.out.println(builder.toString());
		return builder.toString();
	}
	
	public byte[] stringToIntArray(String hashCodeStr){
		byte[] retval = null;
		if(hashCodeStr == null || hashCodeStr == ""){
			return retval;
		}
		retval = new byte[hashCodeStr.length()];
		int idx = 0;
		for(char c:hashCodeStr.toCharArray()){
			retval[idx] = Byte.parseByte(Character.toString(c));
			idx++;
		}
		return retval;
	}
	
	
	/**
	 * 时间点右边 delta T的时间 根据timeHash的位数做判断
	 * 
	 * @param timeHash
	 * @param plusOrMinus  plus:true  minus:false
	 * @return
	 * 
	 */
	String deltaTNearBy(String timeHash,boolean plusOrMinus){
		String retval = "";
		if(timeHash == null || timeHash == ""){
			return retval;
		}
		int oriPrecision = timeHash.length();
		int fillPrecision = MAX_PRECISION_16 - oriPrecision+1;
		
		String completeTimeHash = rightFill(timeHash,MAX_PRECISION_16, '0');
		String deltaTTimeHash = leftFill(rightFill("1",fillPrecision, '0'), MAX_PRECISION_16, '0');
		
		// to decimal
		int minusDec = Integer.parseInt(completeTimeHash,4) - Integer.parseInt(deltaTTimeHash,4);
		if(plusOrMinus){
			minusDec = Integer.parseInt(completeTimeHash,4) + Integer.parseInt(deltaTTimeHash,4);
		}
		
		// decimal to binary
		long ret = bitToTime(stringToIntArray(leftFill(Integer.toBinaryString(minusDec), BIT_NUMBER_32, '0')));
		System.out.println("deltaT:"+ret);
		return normalize(ret,oriPrecision);
	}
	
	/**
	 * 时间点右边 delta T的时间 根据timeHash的位数做判断
	 * @return left nearby time hash code
	 */
	public String leftNearBy(String timeHash){
		return deltaTNearBy(timeHash,false);
	}
	
	/**
	 * 时间点右边 delta T的时间 根据timeHash的位数做判断
	 * @param timeHash
	 * @return
	 */
	public String rightNearBy(String timeHash){
		return deltaTNearBy(timeHash,true);
	}
	
}
