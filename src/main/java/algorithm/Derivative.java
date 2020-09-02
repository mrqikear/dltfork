package algorithm;

import java.util.ArrayList;

/**
 * 连续点求导
 * △x
 * f(x + △x) -f(x)
 *-----------------
 *      △x
 *      df △x  =  f(x1)' +(fx2)' + f(x3)'.... + f(xn)'
 */

public class Derivative {

    /**
     返回判断结果
     * @return
     * TYPE : 1  A   2 B  3 C
     */
    public static  boolean Result(ArrayList<Integer> dataArr ,int TYEP){
         switch (TYEP){
             case 1:
                  return CheckA(dataArr);
             case 2:
                 break;
             case 3:
                 break;
         }
        return false;
    }





 private static boolean CheckA(ArrayList<Integer> point ){
     /**
      * 18-28 第一个波峰
      * 第一波形点
      * 18-28 检测第一个波峰
      *
      * 中间点
      * 23
      * 左推10个点
      * 波形趋势左边
      *  8 -23
      * 右推10个点
      * 23-38
      * 波形右边趋势
      /**
      * 162-172 第二个波峰
      * 中间点 167
      *   左右各推10个点
      * 左边趋势
      * 152-167
      * 右边趋势
      *  167- 182
      */

     /**
      * 第三个波峰
      * 中间点  310
      * 左右各推10个点
      *  305-315 第三个波峰
      * 左边趋势
      *   295-310
      * 右边趋势
      * 310 - 325
      */
     ArrayList<Integer> oneLeft = new ArrayList<>();
     ArrayList<Integer> oneArrLeftHafl = new ArrayList<>();
     ArrayList<Integer> oneArrRightHafl = new ArrayList<>();
     ArrayList<Integer> oneRight = new ArrayList<>();
     ArrayList<Integer> twoLeft = new ArrayList<>();
     ArrayList<Integer> twoArrLeftHalf = new ArrayList<>();
     ArrayList<Integer> twoArrRightHalf = new ArrayList<>();
     ArrayList<Integer> twoRight = new ArrayList<>();
     ArrayList<Integer> treeLeft = new ArrayList<>();
     ArrayList<Integer> treeArrLeftHalf = new ArrayList<>();
     ArrayList<Integer> treeArrRightHalf = new ArrayList<>();
     ArrayList<Integer> treeRight = new ArrayList<>();


     //各个区间导数合集
     ArrayList <Integer>  dfOneLeftHalf   = new ArrayList<>();
     ArrayList <Integer>  dfOneRightHalf   = new ArrayList<>();
     ArrayList <Integer>  dfTwoLeftHalf   = new ArrayList<>();
     ArrayList <Integer>  dfTwoRightHalf   = new ArrayList<>();
     ArrayList <Integer>  dfTreeLeftHalf   = new ArrayList<>();
     ArrayList <Integer>  dfTreeRightHalf   = new ArrayList<>();

     /**
      * 第一个脉冲点
      */
     int start1 = 18;
     int mid1 = 23;
     int end1 = 28;

     /**
      * 第二个脉冲点
      * 162-172 第二个波峰
      *          * 中间点 167
      *          *   左右各推10个点
      *          * 左边趋势
      *          * 152-167
      *          * 右边趋势
      *         *  167- 182
      */
     int start2 = 162;
     int mid2= 167;
     int end2 = 172;

     /**
      第三个波峰
      * 中间点  310
      * 左右各推10个点
      *  305-315 第三个波峰
      * 左边趋势
      *   295-310
      * 右边趋势
      * 310 - 325
      */

     int start3 = 305;
     int mid3 = 310;
     int end3 = 315;

     //左边大范围
     for (int i = 0; i < point.size(); i++) {

         if (i >= start1 -10 && i <= mid1) {
             oneLeft.add(point.get(i));
         }
         if (i >= start1  && i <= mid1) {    //左边趋势
             oneArrLeftHafl.add(point.get(i));
         }
         if (i >= mid1 && i <= end1 ) {  //右边趋势
             oneArrRightHafl.add(point.get(i));
         }

         if (i >= mid1 && i <= end1+10) {
             oneRight.add(point.get(i));
         }
         /**
          * 第二点
          */
         if (i >= start2 -10 && i <= mid2) {
             twoLeft.add(point.get(i));
         }

         //第二个点左边趋势
         if (i >= start2  && i <= mid2) {
             twoArrLeftHalf.add(point.get(i));
         }
         //第二个点右边趋势
         if (i >= mid2 && i <= end2) {
             twoArrRightHalf.add(point.get(i));
         }

         if (i >= mid2 && i <= end2+10) {
             twoRight.add(point.get(i));
         }
         /**
          * 第三个波峰
          */
         if(i>= start3 -10 && i<=mid3){
             treeLeft.add(point.get(i));
         }

         if(i> start3  && i< mid3){
             treeArrLeftHalf.add(point.get(i));
         }

         if(i>=mid3  && i<=end3){
             treeArrRightHalf.add(point.get(i));
         }

         if(i>=mid3 && i<=end3+10){
             treeRight.add(point.get(i));
         }
     }




     return  false;
    }

}
