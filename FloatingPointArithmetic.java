import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class FloatingPointArithmetic {
    private HashMap<Character, Double> symbolAndProbabilityMap;
    HashMap<Character, List<BigDecimal>> SymbollowHighRanges;
    private String inputData;
    BigDecimal  compressionCode=BigDecimal.ZERO;
    String output;
    String compressionCodeFilePath="compressionCode.txt";





    public FloatingPointArithmetic(String inputData){
        this.inputData=inputData;
        this.symbolAndProbabilityMap= new HashMap<>();
        symbolAndProbabilityMap= calculateProbability();
        this.SymbollowHighRanges= new HashMap<>();
        SymbollowHighRanges=CalculateLow_HighRanges(symbolAndProbabilityMap);
        this.compressionCode= compression(SymbollowHighRanges);
        this.output=deCompression(compressionCodeFilePath);

    }

    public HashMap<Character, Double> calculateProbability(){
        HashMap<Character, Integer> tempMap= new HashMap<>();
        int totalNumberOfCharacters=inputData.length();
        for(int i=0; i<inputData.length();i++){
            char c=inputData.charAt(i);
            tempMap.put(c, tempMap.getOrDefault(c,0)+1);
        }
        for(Map.Entry<Character, Integer> myEntry: tempMap.entrySet()){
            int freq=myEntry.getValue();
            double probability= (double) freq /totalNumberOfCharacters;
            symbolAndProbabilityMap.put(myEntry.getKey(), probability);
        }

        for(Map.Entry<Character, Double> myEntry: symbolAndProbabilityMap.entrySet()){
            System.out.println("character: "+myEntry.getKey()+" probability:" +myEntry.getValue());

        }
        return symbolAndProbabilityMap;
    }

    public HashMap<Character, List<BigDecimal>> CalculateLow_HighRanges(HashMap<Character, Double> probabilityMap){
        HashMap<Character, List<BigDecimal>> lowHighRangesOfSymbol= new HashMap<>();
        int first=0;
        BigDecimal prevHighRange = BigDecimal.ZERO;
        BigDecimal lowRange = BigDecimal.ZERO;
        BigDecimal highRange = BigDecimal.ZERO;

        int i=0;
        List<BigDecimal> ranges;
        for(Map.Entry<Character, Double> myEntry: probabilityMap.entrySet()){
            char ch= myEntry.getKey();
            BigDecimal probability = BigDecimal.valueOf(myEntry.getValue());
            ranges =new ArrayList<>();

            if(first==0){
                lowRange=BigDecimal.ZERO;
                highRange= probability;
                prevHighRange=highRange;
                ranges.add(lowRange.setScale(10, RoundingMode.HALF_UP));
                ranges.add(highRange.setScale(10, RoundingMode.HALF_UP));
                lowHighRangesOfSymbol.put(ch,ranges);
                first++;
                i++;
            }
            else{

                lowRange= prevHighRange;
                highRange=lowRange.add(probability);
                prevHighRange=highRange;
                ranges.add(lowRange.setScale(10, RoundingMode.HALF_UP));
                ranges.add(highRange.setScale(10, RoundingMode.HALF_UP));
                lowHighRangesOfSymbol.put(ch,ranges);
            }
        }
        for (Map.Entry<Character, List<BigDecimal>> entry : lowHighRangesOfSymbol.entrySet()) {
            System.out.println("Character: " + entry.getKey() + ", Ranges: " + entry.getValue());
        }
        return lowHighRangesOfSymbol;

    }

    public BigDecimal  compression(HashMap<Character, List<BigDecimal>> lowAndHighRanges) {
        int count = 0;
        BigDecimal prevLower = BigDecimal.ZERO;
        BigDecimal prevRange = BigDecimal.ZERO;
        BigDecimal lower = BigDecimal.ZERO;
        BigDecimal upper = BigDecimal.ONE;
        BigDecimal compressionCode = BigDecimal.ZERO;

        for (int i = 0; i < inputData.length(); i++) {
            char currentChar = inputData.charAt(i);
            Iterator<Map.Entry<Character, List<BigDecimal>>> itr = lowAndHighRanges.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<Character, List<BigDecimal>> myEntry = itr.next();
                char ch = myEntry.getKey();

                if (ch == currentChar) {
                    List<BigDecimal> ranges = myEntry.getValue();
                    BigDecimal lowRange = ranges.get(0);
                    BigDecimal highRange = ranges.get(1);

                    if (count == 0) {
                        lower = lowRange;
                        upper = highRange;
                        prevLower = lower;
                        prevRange = upper.subtract(lower);

                        count++;
                        break;
                    } else {
                        lower = prevLower.add(prevRange.multiply(lowRange));
                        upper = prevLower.add(prevRange.multiply(highRange));
                        prevLower = lower;
                        prevRange = upper.subtract(lower);

                        if (i == inputData.length() - 1) {
                            compressionCode = lower.add(upper).divide(BigDecimal.valueOf(2));
                        }
                        break;
                    }
                }
            }
        }
        try (FileWriter writer = new FileWriter(compressionCodeFilePath)) {
            writer.write(compressionCode.toString() + "\n");
            writer.write(inputData.length() + "\n");

            for (Map.Entry<Character, List<BigDecimal>> entry : lowAndHighRanges.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue().get(0) + "," + entry.getValue().get(1) + "\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return compressionCode;
    }


    public Map<String, Object> readForDecompression(String compressionFilePath){
        HashMap<Character, List<BigDecimal>> newRangesMap= new HashMap<>();
        Map<String, Object> result = new HashMap<>();
        int length=0;
        BigDecimal code = BigDecimal.ZERO;

        try (Scanner scanner = new Scanner(new File(compressionFilePath))) {
            if (scanner.hasNextLine()) {
                code = new BigDecimal(scanner.nextLine().trim());
            }
            if (scanner.hasNextLine()) {
                length = Integer.parseInt(scanner.nextLine().trim());
            }
            String[] parts;
            String[] rangeParts;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                char symbol=' ';
                if (line.isEmpty()) {
                    if (scanner.hasNextLine()) {
                        String nextLine = scanner.nextLine().trim();
                        parts = nextLine.split(":");
                        if (parts.length == 2) {
                            String symbolStr = "\\n";
                            rangeParts = parts[1].split(",");
                            if (rangeParts.length == 2) {
                                try {
                                    BigDecimal lowRange = new BigDecimal(rangeParts[0].trim());
                                    BigDecimal highRange = new BigDecimal(rangeParts[1].trim());
                                    newRangesMap.put('\n', Arrays.asList(lowRange, highRange));
                                } catch (NumberFormatException e) {
                                    System.err.println("Error parsing range for newline symbol");
                                }
                            }
                        }
                    }
                    continue;
                }
                parts = line.split(":");
                if (parts.length != 2) {
                    continue;
                }
                String symbolStr = parts[0];
                symbol = symbolStr.charAt(0);

                rangeParts = parts[1].split(",");
                if (rangeParts.length != 2) {
                    continue;
                }
                try {
                    BigDecimal lowRange = new BigDecimal(rangeParts[0].trim());
                    BigDecimal highRange = new BigDecimal(rangeParts[1].trim());
                    newRangesMap.put(symbol, Arrays.asList(lowRange, highRange));
                } catch (NumberFormatException e) {
                    System.err.println("Error parsing range for symbol: " + symbol);
                }
            }
        }catch (FileNotFoundException e) {
            System.err.println("Compression file not found.");
            e.printStackTrace();

        }
        result.put("ranges", newRangesMap);
        result.put("code", code);
        result.put("length", length);
        return result;

    }

    public String deCompression(String compressionFilePath){
        Map<String, Object> decompressionData=readForDecompression(compressionFilePath);
        HashMap<Character, List<BigDecimal>> newRangesMap= new HashMap<>();
        newRangesMap= (HashMap<Character, List<BigDecimal>>) decompressionData.get("ranges");
        int length= (int) decompressionData.get("length");
        BigDecimal code = (BigDecimal) decompressionData.get("code");

        StringBuilder outputBuilder = new StringBuilder();
        int count= 0;
        BigDecimal lower = BigDecimal.ZERO;
        BigDecimal upper = BigDecimal.ONE;
        BigDecimal prevLower = BigDecimal.ZERO;
        BigDecimal prevRange = BigDecimal.ONE;
        BigDecimal originalCode = code;


        Iterator<Map.Entry<Character, List<BigDecimal>>> itr = newRangesMap.entrySet().iterator();
        while (length>0&&itr.hasNext()) {
            Map.Entry<Character, List<BigDecimal>> myEntry = itr.next();
            List<BigDecimal> ranges = myEntry.getValue();
            BigDecimal lowRange = ranges.get(0);
            BigDecimal highRange = ranges.get(1);

            if (code.compareTo(lowRange) >= 0 && code.compareTo(highRange) < 0) {
                    char currentChar = myEntry.getKey();
                    outputBuilder.append(currentChar);
                    length--;
                    if (count== 0) {
                        lower= lowRange;
                        upper= highRange;
                        prevLower= lower;
                        prevRange= upper.subtract(lower);
                        count++;

                    } else {
                        lower= prevLower.add(prevRange.multiply(lowRange));
                        upper= prevLower.add(prevRange.multiply(highRange));
                        prevLower=lower;
                        prevRange=upper.subtract(lower);
                    }
                    code=  (originalCode.subtract(lower)).divide(prevRange, MathContext.DECIMAL128);
                    itr= newRangesMap.entrySet().iterator();
                }
            }
        try{
            FileWriter writer = new FileWriter("output.txt");
            for(int i=0;i<outputBuilder.length();i++){
                writer.write(outputBuilder.charAt(i));
            }
            writer.close();

        }catch(Exception e){
            e.printStackTrace();
        }
        return outputBuilder.toString();
    }

    public static void main(String[] args){
        String data="";
        try{
            File fileobject= new File("input.txt");
            Scanner fileReader= new Scanner(fileobject);
            while(fileReader.hasNextLine()){
                data+=fileReader.nextLine()+"\n";
            }
        }catch(FileNotFoundException e){
            System.out.println("File not found");
            e.printStackTrace();
        }

        System.out.println(data);

        FloatingPointArithmetic floatingPointArithmetic= new FloatingPointArithmetic(data);
        String compressionCode= String.valueOf(floatingPointArithmetic.compressionCode);
        String output=floatingPointArithmetic.output;
        System.out.println("compression Code: "+ compressionCode);
        System.out.println("Decompression Output: "+output);

    }
}
