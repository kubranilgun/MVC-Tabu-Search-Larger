import java.io.*;
import java.lang.System;
import java.util.*;
import java.util.List;

public class Main {

    //Public değişkenler tanımlanır:
    public static Random rand = new Random();
    public static int[] vertices;
    public static int[][] edges;

    //Tabu listesi (kısa dönemli hafıza) tanımlanır:
    public static List<Tabu> shortTermMemory = new ArrayList<>();

    //Uzun dönemli hafıza tanımlanır:
    public static List<boolean[]> longTermMemory = new ArrayList<>();

    //En iyi çözüm tanımlanır:
    public static boolean[] bestSolution;

    public static void main(String[] args) throws IOException {

        //Çözülecek problem seçilir:
        selectProblem();

        //Başlangıç çözümü üretilir:
        bestSolution = generateInitialSolution();

        //Başlangıç çözümü uzun dönemli hafızaya alınır:
        longTermMemory.add(bestSolution);

        //Başlangıç çözümünün ceza puanı ekrana basılır:
        int penaltyPoint = evaluateSolution(bestSolution);
        System.out.println("Penalty point of initial solution: " + penaltyPoint + " \n___________________________");

        //Durma şartı sağlanana kadar en iyi çözüm aranır:
        boolean[] candidateSolution;
        boolean [] currentSolution;
        List<boolean[]> neighbours;
        List<Integer> markedVertices = new ArrayList<>();
        int iteration = 0;
        while(!checkStopCondition()) {
            iteration++;
            System.out.println("Iteration number: " + (iteration+1));

            //Komşu çözümler belirlenir:
            neighbours = generateNeighbours();

            //En iyi komşu çözüm seçilerek aday çözüm yapılır:
            candidateSolution = selectBestNeighbour(neighbours);

            //Aday çözüm tabu değilse veya tabu yıkma kriterini karşılıyorsa aşağıdaki adımlara geçilir:
            if (!checkTabuCondition(candidateSolution) || checkTabuBreak(candidateSolution)){

                //Aday çözüm mevcut çözüm yapılır:
                currentSolution = candidateSolution;

                //Aday çözüm en az en iyi çözüm kadar iyiyse, en iyi çözüm yapılır:
                if (evaluateSolution(candidateSolution) <= evaluateSolution(bestSolution)) {
                    bestSolution = candidateSolution;
                }

                //İterasyondaki en iyi çözümün ceza puanı ekrana basılır:
                penaltyPoint = evaluateSolution(bestSolution);
                System.out.println("Penalty point of best solution: " + penaltyPoint + " \n___________________________");

                //Eklenmesi gereken tabu varsa tabu listesi güncellenir:
                updateTabuList(currentSolution);

                //En iyi çözüm uzun dönemli hafızaya eklenir:
                longTermMemory.add(bestSolution);
            }
        }

        //Nihai çözümün ceza puanı ekrana basılır:
        penaltyPoint = evaluateSolution(bestSolution);
        System.out.println("Penalty point of ultimate solution: " + penaltyPoint + " \n___________________________");
    }

    //Her kenarın iki ucundan rastgele bir düğüm seçilerek başlangıç çözümü üretilir:
    public static boolean[] generateInitialSolution(){

        //Problemde verilen grafiğin bütün kenarları dolaşılır, her bir kenarın iki ucundaki düğümlerden bir tanesi rastgele seçilir:
        List<Integer> traversedVertices = new ArrayList<>();
        for(int i=0; i<edges.length; i++){
            traversedVertices.add(edges[i][rand.nextInt(2)]);
        }

        //Rastgele seçilirken yinelenen düğümlerin çıkarılır, işaretlenecek düğümler belirlenir:
        List<Integer> selectedVertices = new ArrayList<>();
        for(int i=0; i<traversedVertices.size(); i++){
            boolean exist = false;
            for (int j=0; j<selectedVertices.size(); j++){
                if(traversedVertices.get(i) == selectedVertices.get(j)){
                    exist=true;
                    break;
                }
            }
            if (!exist)selectedVertices.add(traversedVertices.get(i));
        }

        //İşaretlenecek düğümler true, diğer düğümler false olacak şekilde set edilerek, başlangıç çözümü oluşturulur:
        boolean[] initialSolution = new boolean[vertices.length];
        for(int i=0; i<vertices.length; i++){
            boolean exist = false;
            for(int j=0; j<selectedVertices.size(); j++) {
                if(vertices[i]==selectedVertices.get(j)){
                    exist = true;
                    break;
                }
            }
            if(exist) initialSolution[i] = true;
            else initialSolution[i] = false;
        }

        return initialSolution;
    }

    //En iyi çözümün rastgele bir düğümü değiştirilerek komşu çözüm listesi oluşturulur:
    public static List<boolean[]> generateNeighbours(){
        boolean[] candidate;
        List<boolean[]> neighbours = new ArrayList<>();
        int randInt;
        for(int i=0; i<vertices.length; i++) {
            candidate = Arrays.copyOf(bestSolution, bestSolution.length);
            randInt = rand.nextInt(vertices.length);
            candidate[randInt] = !candidate[randInt];
            neighbours.add(candidate);
        }

        return neighbours;
    }

    //Komşu çözüm listesindeki en iyi komşu belirlenir:
    public static boolean[] selectBestNeighbour(List<boolean[]> neighbours){
        boolean[] bestNeighbour = neighbours.get(0);
        for(int i=1; i<neighbours.size(); i++) {
            if (evaluateSolution(neighbours.get(i))<evaluateSolution(bestNeighbour)){
                bestNeighbour = neighbours.get(i);
                break;
            }
        }

        return bestNeighbour;
    }

    //Aday çözümdeki değişikliğin tabu listesinde olup olmadığına bakılır:
    public static boolean checkTabuCondition(boolean[] candidateSolution){
        boolean tabu = false;
        for(int i=0; i<shortTermMemory.size(); i++){
            int vertexPlace = shortTermMemory.get(i).getVertexPlace();
            boolean changedTo = shortTermMemory.get(i).getChangedTo();

            //Yapılan değişikliğin tabu listesinde olup olmadığına bakılır:
            if(bestSolution[vertexPlace]==candidateSolution[vertexPlace]){
                if(bestSolution[vertexPlace]==!changedTo && (candidateSolution[vertexPlace])==changedTo)
                    tabu = true;
                break;
            }
        }

        return tabu;
    }

    //Aday çözümün tabu yıkma kriterini sağlayıp sağlamadığına bakılır:
    public static boolean checkTabuBreak(boolean[] candidateSolution){

        //Aday çözüm en az en iyi çözüm kadar iyiyse, tabu yıkma kriterini sağlamış sayılır:
        boolean breakTabu = false;
        if(evaluateSolution(candidateSolution)<evaluateSolution(bestSolution)){
            breakTabu = true;
        }

        return breakTabu;
    }

    //En iyi çözümde değişiklik olduysa, tabu listesi güncellenir:
    public static void updateTabuList(boolean[] newBestSolution){
        for(int i=0; i<vertices.length; i++){
            if(bestSolution[i]!=newBestSolution[i]){

                //Tabu listesindeki eleman sayısı problemdeki düğüm sayısının 15'te birine ulaşmışsa, ilk eleman tabu listesinden çıkarılır:
                if (shortTermMemory.size()/15==vertices.length) {
                    shortTermMemory.remove(0);
                }

                //Yeni tabu, tabu listesine eklenir:
                Tabu tabu = new Tabu();
                tabu.setVertexPlace(i);
                tabu.setChangedTo(newBestSolution[i]);
                shortTermMemory.add(tabu);
                break;
            }
        }
    }

    //Gelen çözüm grafikteki tüm kenarlara ulaşılma durumu ve örtülmüş düğüm sayısına göre değerlendirilir:
    public static int evaluateSolution (boolean[] solution){

        //Çözümdeki işaretli düğümlerin listesi çıkarılır:
        List<Integer> markedVertices = new ArrayList<>();
        for(int i=0; i<vertices.length; i++){
            if (solution[i]){
                int vertex = vertices[i];
                markedVertices.add(vertex);
            }
        }

        //Çözümün grafikteki tüm kenarlara ulaşıp ulaşmadığına bakılır:
        boolean graphCovered = true;
        for(int i=0; i<edges.length; i++){
            boolean edgeCovered = false;
            for(int j=0; j<markedVertices.size(); j++) {
                if (edges[i][0] == markedVertices.get(j) || edges[i][1] == markedVertices.get(j)){
                    edgeCovered = true;
                    break;
                }
            }
            if (!edgeCovered) {
                graphCovered = false;
                break;
            }
        }

        //Çözüm tüm kenarlara ulaşmıyorsa, çözüme problemdeki düğüm sayısının 10 katı kadar ceza puanı verilir:
        int penaltyPoint;
        if (graphCovered) penaltyPoint = 0;
        else penaltyPoint = vertices.length * 10;

        //Ceza puanı, çözümde seçili düğüm sayısı kadar arttırılır:
        penaltyPoint = penaltyPoint + markedVertices.size();

        return penaltyPoint;
    }

    //Çözülecek problem seçilir:
    public static void selectProblem() throws IOException {

        //Kullanıcıdan dosya seçmesi istenir:
        System.out.println("Lütfen bir dosya seçiniz: ");
        int selectedIndex = 1;
        File directory = new File("res".concat(File.separator));
        File[] dataFiles = directory.listFiles();
        if(dataFiles.length > 0) {
            for(File dataFile:dataFiles) {
                System.out.println(selectedIndex + ". " + dataFile.getName());
                selectedIndex++;
            }
        }
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        String selectedIndexStr = reader.readLine();
        try {
            selectedIndex = Integer.parseInt(selectedIndexStr);
        } catch (Exception e) {
            System.err.println("Geçersiz seçenek!");
            System.exit(0);
        }
        selectedIndex = selectedIndex - 1;
        if(selectedIndex >= dataFiles.length || selectedIndex < 0) {
            System.err.println("Geçersiz seçenek!");
            System.exit(0);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(dataFiles[selectedIndex]))) {
            int lineNumber = 1;
            String line;
            do {
                line = br.readLine();
                if(line != null && !line.isEmpty()) {
                    String[] splitt = line.split("\\s");
                    if(lineNumber == 1) {
                        vertices = new int[Integer.parseInt(splitt[2])];
                        edges = new int[Integer.parseInt(splitt[3])][2];
                        for(int i = 0; i < vertices.length; i++) {
                            vertices[i] = (i + 1);
                        }
                    } else {
                        edges[(lineNumber-2)][0] = Integer.parseInt(splitt[1]);
                        edges[(lineNumber-2)][1] = Integer.parseInt(splitt[2]);
                    }
                    lineNumber++;
                }
            } while(line != null);
        }
    }

    //İterasyonun durma şartı kontrol edilir:
    public static boolean checkStopCondition(){

        //En iyi çözümün ceza puanı, problemdeki düğüm sayısının 15'te biri kadar iterasyon boyunca sabit kaldıysa, durma şartı sağlanmış sayılır:
        boolean stop = true;
        if (longTermMemory.size()>=vertices.length/15){
            for(int i=longTermMemory.size()-vertices.length/15; i<longTermMemory.size()-1; i++){
                if(longTermMemory.get(i)!=longTermMemory.get(i+1)) {
                    stop=false;
                    break;
                }
            }
        } else{
            stop = false;
        }

        return stop;
    }
}