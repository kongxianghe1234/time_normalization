package com.lzy.titan.time_normalization;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.sql.Timestamp;

import org.junit.Before;
import org.junit.Test;

public class TimeNormalizationTest {

	TimeNormalization timeNor;
	
	@Before
	public void init(){
		timeNor = new TimeNormalization();
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void  testEncodeBase4_error01(){
		byte[] arr = {1,2,3,3,12};
		timeNor.encodeBase4(arr);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void  testEncodeBase4_error02(){
		byte[] arr = null;
		timeNor.encodeBase4(arr);
	}
	
	@Test
	public void testEncodeBase4(){
		{
			byte[] arr = {1,0,1,1};
			assertEquals("23", timeNor.encodeBase4(arr));
		}
		{
			byte[] arr = {0,0,1,0,1,1,1,0};
			assertEquals("0232", timeNor.encodeBase4(arr));
		}
		{
			byte[] arr = {1,0,0,1};
			assertEquals("21", timeNor.encodeBase4(arr));
		}
	}
	
	@Test
	public void testDecodeBase4(){
		assertEquals("11100001", intArrayToString(timeNor.decodeBase4("3201")));
		assertEquals("11", intArrayToString(timeNor.decodeBase4("3")));
		assertEquals("00", intArrayToString(timeNor.decodeBase4("0")));
		assertEquals("011011100110", intArrayToString(timeNor.decodeBase4("123212")));
		assertEquals("101010010101111111", intArrayToString(timeNor.decodeBase4("222111333")));
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void testDecodeBase4_error1(){
		assertEquals("11100001", intArrayToString(timeNor.decodeBase4("324301")));
	}
	
	@Test
	public void testNormalize(){
		// 0 min (1500002786-1499999777) = 3009 ~= 50min (10位一样1321223233)
		assertEquals("1321223233000000", timeNor.normalize(1499999777l));
		assertEquals("1321223233010233", timeNor.normalize(1500000000l));
		assertEquals("1321223233010302", timeNor.normalize(1500000002l));
		assertEquals("1321223233010331", timeNor.normalize(1500000010l));
		assertEquals("1321223233011022", timeNor.normalize(1500000020l));
		assertEquals("1321223233011030", timeNor.normalize(1500000021l));
		assertEquals("1321223233012030", timeNor.normalize(1500000068l));
		assertEquals("1321223233012032", timeNor.normalize(1500000070l));
		assertEquals("1321223233012131", timeNor.normalize(1500000081l));
		assertEquals("1321223233022003", timeNor.normalize(1500000250l));
		assertEquals("1321223233022013", timeNor.normalize(1500000253l));
		assertEquals("1321223233112131", timeNor.normalize(1500000833l));
		// 50 min 
		assertEquals("1321223233333333", timeNor.normalize(1500002786l));
		assertEquals("1321223300012222", timeNor.normalize(1500003100l));
		assertEquals("1321223300021133", timeNor.normalize(1500003233l));
		assertEquals("1321223300112031", timeNor.normalize(1500003831l));
	}
	
	@Test
	public void testDeNormalize(){
		// delta T < 1s | double * 2^32 might be error
		assertEquals(1499999777l, timeNor.deNormalize("1321223233000000").longValue(),1);
		assertEquals(1500000000l, timeNor.deNormalize("1321223233010233").longValue(),1);
		assertEquals(1500000021l, timeNor.deNormalize("1321223233011030").longValue(),1);
		assertEquals(1500002786l, timeNor.deNormalize("1321223233333333").longValue(),1);
		assertEquals(1500003233l, timeNor.deNormalize("1321223300021133").longValue(),1);
		
		// random test
		for(int i =0;i<1000000;i++){
			long input = Math.round(Math.random()*10000000)+1500000000;
			assertEquals(input, timeNor.deNormalize(timeNor.normalize(input)),1);;
		}
	}
	
	@Test
	public void testTimeRange_8(){
		Timestamp ts1 = new Timestamp(timeNor.deNormalize("1321223231111111").longValue()*1000);
		Timestamp ts2 = new Timestamp(timeNor.deNormalize("1321223331111111").longValue()*1000);
		System.out.println(ts1.toLocalDateTime().toString());
		System.out.println(ts2.toLocalDateTime().toString());
		// 前8位一样基本时间差 13.3 hours
	}
	
	@Test
	public void testTimeRange_9(){
		Timestamp ts1 = new Timestamp(timeNor.deNormalize("1321223233000000").longValue()*1000);
		Timestamp ts2 = new Timestamp(timeNor.deNormalize("1321223223000000").longValue()*1000);
		System.out.println(ts1.toLocalDateTime().toString());
		System.out.println(ts2.toLocalDateTime().toString());
		// 前9位一样基本时间差 200 minutes
	}
	
	@Test
	public void testTimeRange_10(){
		Timestamp ts1 = new Timestamp(timeNor.deNormalize("1321223233000000").longValue()*1000);
		Timestamp ts2 = new Timestamp(timeNor.deNormalize("1321223232000000").longValue()*1000);
		System.out.println(ts1.toLocalDateTime().toString());
		System.out.println(ts2.toLocalDateTime().toString());
		// 前10位一样基本时间差 50 minutes
	}
	
	@Test
	public void testTimeRange_11(){
		Timestamp ts1 = new Timestamp(timeNor.deNormalize("1321223233000000").longValue()*1000);
		Timestamp ts2 = new Timestamp(timeNor.deNormalize("1321223233033333").longValue()*1000);
		System.out.println(ts1.toLocalDateTime().toString());
		System.out.println(ts2.toLocalDateTime().toString());
		// 前11位一样基本时间差 12 minutes
	}
	
	@Test
	public void testTimeRange_12(){
		Timestamp ts1 = new Timestamp(timeNor.deNormalize("1321223233023333").longValue()*1000);
		Timestamp ts2 = new Timestamp(timeNor.deNormalize("1321223233020000").longValue()*1000);
		System.out.println(ts1.toLocalDateTime().toString());
		System.out.println(ts2.toLocalDateTime().toString());
		// 前12位一样基本时间差 3 minutes
	}
	
	@Test
	public void testTimeRange_13(){
		Timestamp ts1 = new Timestamp(timeNor.deNormalize("1321223233023333").longValue()*1000);
		Timestamp ts2 = new Timestamp(timeNor.deNormalize("1321223233023000").longValue()*1000);
		System.out.println(ts1.toLocalDateTime().toString());
		System.out.println(ts2.toLocalDateTime().toString());
		// 前13位一样基本时间差 50 seconds
	}
	
	@Test
	public void testTimeRange_14(){
		Timestamp ts1 = new Timestamp(timeNor.deNormalize("1321223233023333").longValue()*1000);
		Timestamp ts2 = new Timestamp(timeNor.deNormalize("1321223233023300").longValue()*1000);
		System.out.println(ts1.toLocalDateTime().toString());
		System.out.println(ts2.toLocalDateTime().toString());
		// 前14位一样基本时间差 12 seconds
	}
	
	@Test
	public void testTimeRange_15(){
		Timestamp ts1 = new Timestamp(timeNor.deNormalize("1321223133023333").longValue()*1000);
		Timestamp ts2 = new Timestamp(timeNor.deNormalize("1321223133023330").longValue()*1000);
		System.out.println(ts1.toLocalDateTime().toString());
		System.out.println(ts2.toLocalDateTime().toString());
		// 前15位一样基本时间差 3 seconds
	}
	
	@Test
	public void testBitToTime() {
		Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:45");
		byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
		Long retval = timeNor.bitToTime(ret);
		//assertEquals(true, Long.compare(ts.getTime()/1000, retval));
		System.out.println(ts.getTime()/1000);
		System.out.println(retval);
		assertEquals(ts.getTime()/1000, retval,1);
		
		
		//random test time
		long offset = Timestamp.valueOf("2018-01-01 00:00:00").getTime();
		long end = Timestamp.valueOf("2019-01-01 00:00:00").getTime();
		long diff = end - offset + 1;
		
		for(int i =0;i<1000000;i++){
			Timestamp rand = new Timestamp(offset + (long)(Math.random() * diff));
			ret = timeNor.timeToBit(rand.getTime()/1000);
			retval = timeNor.bitToTime(ret);
			assertEquals(rand.getTime()/1000, retval,1);	// timestamp间隔小于1s基本上够了
		}
	}
	
	@Test
	public void testTimeToBit() {
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:45");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100101111", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:46");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100110000", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:47");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100110010", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:48");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100110011", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:49");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100110101", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:50");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100110110", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:51");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100110111", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:52");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100111001", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:53");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100111010", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:54");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100111011", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:55");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100111101", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:56");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100111110", str);
		}
		
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:57");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111100111111", str);
		}
		
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:58");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101000001", str);
		}
		
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 11:59:59");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101000010", str);
		}
		
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:00");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101000100", str);
		}
		
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:01");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101000101", str);
		}
		
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:02");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101000110", str);
		}
		
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:03");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101001000", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:04");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101001001", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:05");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101001010", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:06");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101001100", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:07");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101001101", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:08");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101001110", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:09");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101010000", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:10");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101010001", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:11");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101010011", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:12");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101010100", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:13");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101010101", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:14");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101010111", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:00:15");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000101111101011000", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 12:30:00");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000110100011010101", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 13:00:00");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000111001001100111", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 13:16:00");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000111011110000010", str);
		}
		{
			Timestamp ts = Timestamp.valueOf("2019-01-03 13:22:00");
			byte[] ret = timeNor.timeToBit(ts.getTime()/1000);
			String str = intArrayToString(ret);
			assertEquals("01111101011101000111100101101100", str);
		}
		
	}
	
	/**
	 * 算法性能是ok的 2000多万次，5s速度应该还是ok的
	 */
	@Test(timeout=5000)
	public void testTimeToBitSpeed(){
		int count = 0;
		long startTime =  System.currentTimeMillis();
		for(long i=1000000000;i<TimeNormalization.MAX_TIMESTAMP;i+=100){
			timeNor.timeToBit(i);
			count++;
		}
		System.out.println("count:"+count);
		System.out.println("cost "+(System.currentTimeMillis()-startTime)+" ms.");
	}
	
	/**
	 * 性能有点慢，估计是 base4太慢了
	 * TODO(kong) 要做成位运算 --ok
	 */
	@Test(timeout=8000)
	public void testNormalizeSpeed(){
		int count = 0;
		long startTime =  System.currentTimeMillis();
		for(long i=1000000000;i<TimeNormalization.MAX_TIMESTAMP;i+=100){
			timeNor.normalize(i);
			count++;
		}
		System.out.println("count:"+count);
		System.out.println("cost "+(System.currentTimeMillis()-startTime)+" ms.");
	}
	
	
	
	public String intArrayToString(byte[] objs){
		StringBuilder builder = new StringBuilder();
		for(Object obj:objs){
			builder.append(obj);
		}
		//System.out.println(builder.toString());
		return builder.toString();
	}
	
	@Test
	public void testOthers() throws UnsupportedEncodingException{
		byte b = 0;
		int a = b & 0xff;
		System.out.println("byte to int:"+a);
		
		byte a1= 0x1;
		byte a2 = 1;
		System.out.println((a1<<1)+a2);
		
		System.out.println(Integer.toBinaryString(0));
		System.out.println(Integer.toBinaryString(1));
		System.out.println(Integer.toBinaryString(2));
		System.out.println(Integer.toBinaryString(3));
		System.out.println(Byte.parseByte("0"));
		System.out.println(Byte.parseByte("1"));
	}

}
