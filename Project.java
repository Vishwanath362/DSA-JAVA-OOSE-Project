import java.io.*;
import java.util.*;

public class Project {

    static class Process {
        int pid, arrival, burst, priority, remaining;
        Process(int pid, int arrival, int burst, int priority) {
            this.pid = pid;
            this.arrival = arrival;
            this.burst = burst;
            this.priority = priority;
            this.remaining = burst;
        }
    }

    public static void main(String[] args) throws Exception {
        List<Process> processes = new ArrayList<>();
        Scanner sc = new Scanner(new File("input.txt"));
        int n = Integer.parseInt(sc.nextLine());
        boolean preemptive = Boolean.parseBoolean(sc.nextLine());

        for (int i = 0; i < n; i++) {
            int pid = Integer.parseInt(sc.nextLine());
            int arrival = convertToSeconds(sc.nextLine());
            int burst = Integer.parseInt(sc.nextLine());
            int priority = preemptive ? Integer.parseInt(sc.nextLine()) : -1;
            processes.add(new Process(pid, arrival, burst, priority));
        }

        double fcfs = fcfsScheduling(cloneList(processes));
        double sjf = sjfScheduling(cloneList(processes));
        double priority = priorityScheduling(cloneList(processes));
        double rr = roundRobinScheduling(cloneList(processes), 2);

        double min = Math.min(Math.min(fcfs, sjf), Math.min(priority, rr));

        if (min == fcfs) System.out.println("FCFS has the least average waiting time: " + fcfs);
        else if (min == sjf) System.out.println("SJF has the least average waiting time: " + sjf);
        else if (min == priority) System.out.println("Priority has the least average waiting time: " + priority);
        else System.out.println("Round Robin has the least average waiting time: " + rr);
    }

    static double fcfsScheduling(List<Process> list) throws IOException {
        list.sort(Comparator.comparingInt(p -> p.arrival));
        int time = 0, totalWT = 0, totalTAT = 0;
        StringBuilder out = new StringBuilder("PID\tAT\tBT\tWT\tTAT\n");

        for (Process p : list) {
            time = Math.max(time, p.arrival);
            int wt = time - p.arrival;
            int tat = wt + p.burst;
            time += p.burst;
            totalWT += wt;
            totalTAT += tat;
            out.append(p.pid).append("\t").append(p.arrival).append("\t").append(p.burst).append("\t").append(wt).append("\t").append(tat).append("\n");
        }

        double avgWT = totalWT / (double) list.size();
        out.append(String.format("Average Waiting Time: %.2f\n", avgWT));
        writeToFile("fcfs_output.txt", out.toString());
        return avgWT;
    }

    static double sjfScheduling(List<Process> list) throws IOException {
        list.sort(Comparator.comparingInt(p -> p.arrival));
        int time = 0, totalWT = 0, totalTAT = 0;
        StringBuilder out = new StringBuilder("PID\tAT\tBT\tWT\tTAT\n");
        List<Process> ready = new ArrayList<>();
        int completed = 0;

        while (completed < list.size()) {
            for (Process p : list) {
                if (p.arrival <= time && !ready.contains(p)) ready.add(p);
            }
            ready.removeIf(p -> p.remaining == 0);

            if (ready.isEmpty()) {
                time++;
                continue;
            }

            Process next = ready.stream().min(Comparator.comparingInt(p -> p.burst)).get();
            int wt = time - next.arrival;
            int tat = wt + next.burst;
            totalWT += wt;
            totalTAT += tat;
            time += next.burst;
            next.remaining = 0;
            out.append(next.pid).append("\t").append(next.arrival).append("\t").append(next.burst).append("\t").append(wt).append("\t").append(tat).append("\n");
            completed++;
        }

        double avgWT = totalWT / (double) list.size();
        out.append(String.format("Average Waiting Time: %.2f\n", avgWT));
        writeToFile("sjf_output.txt", out.toString());
        return avgWT;
    }

    static double priorityScheduling(List<Process> list) throws IOException {
        list.sort(Comparator.comparingInt(p -> p.arrival));
        int time = 0, totalWT = 0, totalTAT = 0;
        StringBuilder out = new StringBuilder("PID\tAT\tBT\tPRI\tWT\tTAT\n");
        List<Process> ready = new ArrayList<>();
        int completed = 0;

        while (completed < list.size()) {
            for (Process p : list) {
                if (p.arrival <= time && !ready.contains(p)) ready.add(p);
            }
            ready.removeIf(p -> p.remaining == 0);

            if (ready.isEmpty()) {
                time++;
                continue;
            }

            Process next = ready.stream().min(Comparator.comparingInt(p -> p.priority)).get();
            int wt = time - next.arrival;
            int tat = wt + next.burst;
            totalWT += wt;
            totalTAT += tat;
            time += next.burst;
            next.remaining = 0;
            out.append(next.pid).append("\t").append(next.arrival).append("\t").append(next.burst).append("\t")
               .append(next.priority).append("\t").append(wt).append("\t").append(tat).append("\n");
            completed++;
        }

        double avgWT = totalWT / (double) list.size();
        out.append(String.format("Average Waiting Time: %.2f\n", avgWT));
        writeToFile("priority_output.txt", out.toString());
        return avgWT;
    }

    static double roundRobinScheduling(List<Process> list, int quantum) throws IOException {
        list.sort(Comparator.comparingInt(p -> p.arrival));
        Queue<Process> queue = new LinkedList<>();
        int time = 0, totalWT = 0, totalTAT = 0;
        StringBuilder out = new StringBuilder("PID\tAT\tBT\tWT\tTAT\n");
        Map<Integer, Integer> startTimes = new HashMap<>();
        Map<Integer, Integer> completionTimes = new HashMap<>();
        int[] waiting = new int[list.size()];
        int[] turnaround = new int[list.size()];

        while (!list.isEmpty() || !queue.isEmpty()) {
            while (!list.isEmpty() && list.get(0).arrival <= time) {
                queue.add(list.remove(0));
            }

            if (queue.isEmpty()) {
                time++;
                continue;
            }

            Process curr = queue.poll();
            if (!startTimes.containsKey(curr.pid)) {
                startTimes.put(curr.pid, time);
            }

            int exec = Math.min(quantum, curr.remaining);
            curr.remaining -= exec;
            time += exec;

            while (!list.isEmpty() && list.get(0).arrival <= time) {
                queue.add(list.remove(0));
            }

            if (curr.remaining > 0) {
                queue.add(curr);
            } else {
                int wt = time - curr.arrival - curr.burst;
                int tat = time - curr.arrival;
                totalWT += wt;
                totalTAT += tat;
                out.append(curr.pid).append("\t").append(curr.arrival).append("\t").append(curr.burst).append("\t")
                   .append(wt).append("\t").append(tat).append("\n");
            }
        }

        double avgWT = totalWT / (double) startTimes.size();
        out.append(String.format("Average Waiting Time: %.2f\n", avgWT));
        writeToFile("roundrobin_output.txt", out.toString());
        return avgWT;
    }

    static void writeToFile(String filename, String data) throws IOException {
        FileWriter fw = new FileWriter(filename);
        fw.write(data);
        fw.close();
    }

    static int convertToSeconds(String time) {
        String[] parts = time.split(":");
        int h = Integer.parseInt(parts[0]);
        int m = Integer.parseInt(parts[1]);
        int s = Integer.parseInt(parts[2]);
        return h * 3600 + m * 60 + s;
    }

    static List<Process> cloneList(List<Process> original) {
        List<Process> copy = new ArrayList<>();
        for (Process p : original) {
            copy.add(new Process(p.pid, p.arrival, p.burst, p.priority));
        }
        return copy;
    }
}
