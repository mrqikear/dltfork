package algorithm;

/**
 * 卡尔曼滤波实现
 */
public class KalmanFilter {

    int  predict; //原始
    int  current; //过滤后
    double estimate;
    int pdelt;
    int mdelt;
    double Gauss;
    double kalmanGain;
    double Q = 0.00001;
    double R =0.1;

    void initial(){
        this.pdelt =4;
        this.mdelt =3;
    }

    public double KalmanFilter(int  oldValue ,int value){
            this.predict = oldValue;
            this.current = value;
            //高斯噪声方差
           this.Gauss = Math.sqrt(this.pdelt * this.pdelt + this.mdelt*this.mdelt)+this.Q;
            //估计方差
           this.kalmanGain =  Math.sqrt((this.Gauss * this.Gauss)/(this.Gauss * this.Gauss + this.pdelt *this.pdelt)) + this.R;
           this .estimate = (int) (kalmanGain * (current - predict) + predict);
           return  this.estimate;
        }
}
