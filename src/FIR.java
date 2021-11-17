/** A class to perform an FIR filtering operation
    using a circular buffer.
    
    See http://ptolemy.eecs.berkeley.edu/eecs20/week12/implementation.html
**/

class FIR {
    private int length;
    private float[] delayLine;
    private float[] impulseResponse;
    private int count = 0;

    FIR(float[] coefs) {
        length = coefs.length;
        impulseResponse = coefs;
        delayLine = new float[length];
    }

    float getOutputSample(float inputSample) {
        delayLine[count] = inputSample;
        float result = 0;
        int index = count;
        for (int i=0; i<length; i++) {
            result += impulseResponse[i] * delayLine[index--];
            if (index < 0) index = length-1;
        }
        if (++count >= length) count = 0;
        return result;
    }
}
