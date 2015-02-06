import sun.misc.Unsafe;

import java.lang.reflect.Field;

/**
 * Created by sirus20x6 on 10/26/2014.
 */


class UnsafeMemory
{
    private static final Unsafe unsafe;
    static
    {
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static final long byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
    private static final long floatArrayOffset = unsafe.arrayBaseOffset(float[].class);

    private int pos = 0;
    public final byte[] buffer;

    public UnsafeMemory(final byte[] buffer)
    {
        if (null == buffer)
        {
            throw new NullPointerException("buffer cannot be null");
        }

        this.buffer = buffer;
    }

    public void putInt(final int value)
    {
        unsafe.putInt(buffer, byteArrayOffset + pos, value);
       // pos += SIZE_OF_INT;
    }


    public void putFloat(final float value)
    {
        unsafe.putFloat(buffer, byteArrayOffset + pos, value);
        //pos += SIZE_OF_INT;
    }

    public void putFloatAt(final float value,int xy)
    {
        unsafe.putFloat(buffer, byteArrayOffset + xy * 4, value);
        if(pos < xy * 4){
            pos = xy * 4;}
    }
    public float getFloatAt(int xy)
    {
        return  unsafe.getFloat(buffer, byteArrayOffset + xy * 4);
    }
    public void putFloatArray(final float[] values)
    {
        unsafe.copyMemory(values, floatArrayOffset,
                buffer, byteArrayOffset,
                values.length);
        pos = values.length;
    }

    public float[] getFloatArray()
    {
        float[] values = new float[pos];
        unsafe.copyMemory(buffer, byteArrayOffset,
                values, floatArrayOffset,
                pos);
        //pos += bytesToCopy;

        return values;
    }
    public void unsafeCopyTo(UnsafeMemory dest)
    {
        //unsafe.copyMemory(this.byteArrayOffset, dest.byteArrayOffset, pos);
        unsafe.copyMemory(this.buffer, byteArrayOffset, dest.buffer, dest.byteArrayOffset, dest.buffer.length);
        dest.pos = this.buffer.length;
    }
}