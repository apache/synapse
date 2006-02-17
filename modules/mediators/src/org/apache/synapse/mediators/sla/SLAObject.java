package sampleMediators.sla;

public class SLAObject implements Comparable {

    int priority;
    long time;
    Object object;

    public SLAObject() {
    }

    public SLAObject(int priority, long time, Object object) {
        this.priority = priority;
        this.time = time;
        this.object = object;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int index) {
        this.priority = index;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public int compareTo(Object o2) {
        SLAObject obj1 = this;
        SLAObject obj2 = (SLAObject) o2;
        if (obj1.getTime() == obj2.getTime()) {
            if (obj1.getPriority() == obj2.getPriority()) {
                if (obj1.hashCode() == obj2.hashCode()) {
                    return 0;
                } else if (obj1.hashCode() < obj2.hashCode()) {
                    return -1;
                } else return 1;
            } else if (obj1.getPriority() < obj2.getPriority()) {
                return -1;
            } else {
                return 1;
            }
        } else if (obj1.getTime() < obj2.getTime()) {
            return -1;
        } else {
            return 1;
        }
    }
}
