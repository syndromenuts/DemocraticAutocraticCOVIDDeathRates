import java.io.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class CovidDeaths {
    private static String[] top50Countries = {"China", "India", "The United States of America", "Indonesia", "Pakistan", "Brazil", "Nigeria", "Bangladesh", "Russian Federation", "Mexico", "Japan", "Ethiopia", "Philippines", "Egypt", "Vietnam", "Congo", "Turkey", "Iran", "Germany", "Thailand", "The United Kingdom", "France", "Italy", "Tanzania", "South Africa", "Myanmar", "Kenya", "Colombia", "Spain", "Uganda", "Argentina", "Algeria", "Sudan", "Ukraine", "Iraq", "Afghanistan", "Poland", "Canada", "Morocco", "Saudi Arabia", "Uzbekistan", "Peru", "Angola", "Malaysia", "Mozambique", "Ghana", "Yemen", "Nepal", "Venezuela", "Madagascar"};
    private static int countriesToUse = 10;
    public static void main(String[] args) throws IOException{
        // A list of the countries we want
        String[] countries = new String[countriesToUse];
        for(int i = 0; i < countriesToUse; i++) {
            countries[i] = top50Countries[i];
        }

        // Read in WHO-COVID-19-global-data for countries we want to analyze
        ArrayList<ArrayList<String>> countriesData = readNeededCountries(countries);
        // System.out.println(countriesData);
        
        // A list of all of the days we need
        LocalDate[] dates = getDates(countriesData).toArray(new LocalDate[0]);
        System.out.println(Arrays.toString(dates));
        // System.out.println(dates.length);

        // Make a list with only countries and covid deaths
        String[][] countriesCovidDeaths = getDeaths(countriesData, dates.length);
        // System.out.println(Arrays.deepToString(countriesCovidDeaths));
        
        //  At this point we should have series of numbers representing covid deaths for the last few years, with corresponding dates either through a Map, or through multiple arrays maybe        

        // For each of the countries that we care about, we want to read in population sizes for the last few years
        HashMap<String, Double> countryPopulations = readPopulations();
        System.out.println(countryPopulations);
        System.out.println(countryPopulations.size());

        HashMap<String, double[]> deathRates = deathRates(countriesCovidDeaths, countryPopulations, dates);
        // for(Map.Entry<String, double[]> entry: deathRates.entrySet()) {
        //     System.out.println(entry.getKey());
        //     System.out.println(Arrays.toString(entry.getValue()));
        // }

        
        // For each of the countries that we care about, we want to read in the democracy index for the last few years
        HashMap<String, Double> democracyIndex = readDemocracyIndex();
        // System.out.println(democracyIndex);
        
        double[] democracyDeathRatesAgg = democraticPortionDeathRateAgg(democracyIndex, deathRates, dates);

        double[] democracyDeathRate = democraticPortionDeathRate(democracyDeathRatesAgg, democracyIndex);
        // System.out.println(Arrays.toString(democracyDeathRate));

        double[] autocraticDeathRatesAgg = autocraticPortionDeathRateAgg(democracyIndex, deathRates, dates);
        System.out.println(Arrays.toString(autocraticDeathRatesAgg));
        double[] autocraticDeathRate = democraticPortionDeathRate(autocraticDeathRatesAgg, democracyIndex);
        System.out.println(Arrays.toString(autocraticDeathRate));

        // For each of the countries that we care about, we will multiply their covid deaths and their population sizes by their democracy index/10 in order to get the covid deaths and populations associated with the democratic portion of that country
        // HashMap<String, double[]> democraticCovidDeaths = democraticPortionCovidDeaths(democracyIndex, countriesCovidDeaths, dates);
        // for(Map.Entry<String, double[]> entry: democraticCovidDeaths.entrySet()) {
        //     System.out.println(entry.getKey());
        //     System.out.println(Arrays.toString(entry.getValue()));
        // }

        HashMap<String, double[]> democraticPopulations = democraticPortionPopulation(democracyIndex, countryPopulations, dates);
        // for(Map.Entry<String, double[]> entry: democraticPopulations.entrySet()) {
        //     System.out.println(entry.getKey());
        //     System.out.println(Arrays.toString(entry.getValue()));
        // }


        // We will repeat the above step with (10-democracy index)/10 to get the autocratic portions as well
        HashMap<String, double[]> autocraticCovidDeaths = autocraticPortionCovidDeaths(democracyIndex, countriesCovidDeaths, dates);
        // for(Map.Entry<String, double[]> entry: autocraticCovidDeath.entrySet()) {
        //     System.out.println(entry.getKey());
        //     System.out.println(Arrays.toString(entry.getValue()));
        // }

        HashMap<String, double[]> autocraticPopulations = autocraticPortionPopulation(democracyIndex, countryPopulations, dates);
        
        writeFile(democracyDeathRate, autocraticDeathRate, dates);

    }
    // Read in all of the data of needed countries
    public static ArrayList<ArrayList<String>> readNeededCountries(String[] countries) {
        try {
            ArrayList<ArrayList<String>> result = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader("WHO-COVID-19-global-data.csv"));  
            String line;
            while((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                boolean isTopCountries = false;
                for(int i = 0; i < countries.length; i++) {
                    if(row[2].equals(countries[i])) {
                        isTopCountries = true;
                        break;
                    }
                }
    
                if(isTopCountries) {
                    result.add(new ArrayList<String>(Arrays.asList(row)));
                }
            }
            reader.close();
            return result;
        } catch(IOException e) {
            e.printStackTrace();
            return new ArrayList<ArrayList<String>>();
        }
    }

    // Get the dates from the data
    public static ArrayList<LocalDate> getDates(ArrayList<ArrayList<String>> data) {
        ArrayList<LocalDate> dates = new ArrayList<LocalDate>();
        for(int row = 0; row < data.size(); row++) {
            String dateString = data.get(row).get(0);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US);
            LocalDate date = LocalDate.parse(dateString, formatter);
            if(dates.size() > 1 && date.isBefore(dates.get(1))) {
                break;
            } else {
                dates.add(date);
            }
        }
        return dates;
    }

    // Get only date, country, and cumulative deaths for each row
    public static String[][] getDeaths(ArrayList<ArrayList<String>> data, int dates) {
        String[][] result = new String[dates*countriesToUse][3];
        for(int row = 0; row < data.size(); row++) {
            result[row][0] = data.get(row).get(0);
            result[row][1] = data.get(row).get(2);
            result[row][2] = data.get(row).get(7);
        }
        return result;
    }

    // Get democracy index in 2020
    public static HashMap<String, Double> readDemocracyIndex() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("DemocracyIndex.csv"));
            HashMap<String, Double> result = new HashMap<String, Double>();
            
            String line;
            while((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                boolean needed = false;
                for(int i = 0; i < countriesToUse; i++) {
                    if(top50Countries[i].equals(row[0])) {
                        needed = true;
                        break;
                    }
                }
                if(needed && row[2].equals("2020")) {
                    result.put(row[0], Double.parseDouble(row[1]));
                }
            }
            reader.close();
            return result;
        } catch(IOException e) {
            e.printStackTrace();
            return new HashMap<String, Double>();
        }
    }

    // Get populations in 2020
    public static HashMap<String, Double> readPopulations() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("CountryPopulations.csv"));
            HashMap<String, Double> result = new HashMap<String, Double>();

            reader.readLine();
            String line;
            while((line = reader.readLine()) != null) {
                String[] row = line.split(",");
                try {
                    boolean needed = false;
                    for(int i = 0; i < countriesToUse; i++) {
                        if(top50Countries[i].equals(row[1])) {
                            needed = true;
                            break;
                        }
                    }
                    if(needed) {
                        result.put(row[1], Double.parseDouble(row[3])*1000);
                    }
                } catch(NumberFormatException e) {
                    //not a double
                }
            }
            reader.close();
            return result;
        } catch(IOException e) {
            e.printStackTrace();
            return new HashMap<String, Double>();
        }
    }

    // Get the culmulative covid deaths divided by country's population
    public static HashMap<String, double[]> deathRates(String[][] covidDeaths, HashMap<String, Double> populations, LocalDate[] dates) {
        HashMap<String, double[]> deathRates = new HashMap<String, double[]>();
        for(int i = 0; i < covidDeaths.length; i++) {
            LocalDate date = LocalDate.parse(covidDeaths[i][0], DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US));
            String country = covidDeaths[i][1];
            double population = populations.get(country);
            int deaths = Integer.parseInt(covidDeaths[i][2]);
            int days = (int)ChronoUnit.DAYS.between(dates[0], date);
            if(deathRates.containsKey(country)) {
                deathRates.get(country)[days] = deaths/population * 100;
            } else {
                double[] arr = new double[dates.length];
                arr[days] = deaths/population * 100;
                deathRates.put(country, arr);
            }
        }
        return deathRates;
    }

    // Get the democratic portion of Covid Deaths for each country
    // public static HashMap<String, double[]> democraticPortionCovidDeaths(HashMap<String, Double> democraticIndex, String[][] covidDeaths, LocalDate[] dates) {
    //     HashMap<String, double[]> democraticPortionCovidDeaths = new HashMap<String, double[]>();
    //     for(int i = 0; i < covidDeaths.length; i++) {
    //         LocalDate date = LocalDate.parse(covidDeaths[i][0], DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US));
    //         String country = covidDeaths[i][1];
    //         int deaths = Integer.parseInt(covidDeaths[i][2]);
    //         Double democraticIndexCountry = democraticIndex.get(country);
    //         int days = (int)ChronoUnit.DAYS.between(dates[0], date);
    //         if(democraticPortionCovidDeaths.containsKey(country)) {
    //             democraticPortionCovidDeaths.get(country)[days] = deaths*(democraticIndexCountry/10);
    //         } else {
    //             double[] arr = new double[dates.length];
    //             arr[days] = deaths*(democraticIndexCountry/10);
    //             democraticPortionCovidDeaths.put(country, arr);
    //         }
    //     }
    //     return democraticPortionCovidDeaths;
    // }

    public static double[] democraticPortionDeathRateAgg(HashMap<String, Double> democraticIndex, HashMap<String, double[]> deathRates, LocalDate[] dates) {
        HashMap<String, double[]> deathRateAgg = new HashMap<String, double[]>();
        for(String country : deathRates.keySet()) {
            double[] countryRates = deathRates.get(country);
            Double democracyIndex = democraticIndex.get(country);
            double[] countryRatesAgg = new double[countryRates.length];
            for(int i = 0; i < countryRates.length; i++) {
                countryRatesAgg[i] = countryRates[i] * democracyIndex;
            }
            deathRateAgg.put(country, countryRatesAgg);
        }
        double[] deathRateAggSum = new double[dates.length];
        for(String country : deathRateAgg.keySet()) {
            double[] deathRateAggCountry = deathRateAgg.get(country);
            for(int i = 0; i < deathRateAggCountry.length; i++) {
                deathRateAggSum[i] += deathRateAggCountry[i];
            }
        }
        return deathRateAggSum;
    }

    public static double[] democraticPortionDeathRate(double[] deathRateAgg, HashMap<String, Double> democraticIndex) {
        double[] deathRate = new double[deathRateAgg.length];
        double democracyIndexSum = 0;
        for(int i = 0; i < countriesToUse; i++) {
            democracyIndexSum += democraticIndex.get(top50Countries[i]);
        }
        for(int i = 0; i < deathRateAgg.length; i++) {
            deathRate[i] = deathRateAgg[i]/democracyIndexSum;
        }
        return deathRate;
    }

    public static double[] autocraticPortionDeathRateAgg(HashMap<String, Double> democraticIndex, HashMap<String, double[]> deathRates, LocalDate[] dates) {
        HashMap<String, double[]> deathRateAgg = new HashMap<String, double[]>();
        for(String country : deathRates.keySet()) {
            double[] countryRates = deathRates.get(country);
            Double democracyIndex = democraticIndex.get(country);
            double[] countryRatesAgg = new double[countryRates.length];
            for(int i = 0; i < countryRates.length; i++) {
                countryRatesAgg[i] = countryRates[i] * (10-democracyIndex);
            }
            deathRateAgg.put(country, countryRatesAgg);
        }
        double[] deathRateAggSum = new double[dates.length];
        for(String country : deathRateAgg.keySet()) {
            double[] deathRateAggCountry = deathRateAgg.get(country);
            for(int i = 0; i < deathRateAggCountry.length; i++) {
                deathRateAggSum[i] += deathRateAggCountry[i];
            }
        }
        return deathRateAggSum;
    }

    public static double[] autocraticPortionDeathRate(double[] deathRateAgg, HashMap<String, Double> democraticIndex) {
        double[] deathRate = new double[deathRateAgg.length];
        double autocraticIndexSum = 0;
        for(int i = 0; i < countriesToUse; i++) {
            autocraticIndexSum += 10 - democraticIndex.get(top50Countries[i]);
        }
        for(int i = 0; i < deathRateAgg.length; i++) {
            deathRate[i] = deathRateAgg[i]/autocraticIndexSum;
        }
        return deathRate;
    }

    // Get democratic portion of population for each country
    public static HashMap<String, double[]> democraticPortionPopulation(HashMap<String, Double> democraticIndex, HashMap<String, Double>  populations, LocalDate[] dates) {
        HashMap<String, double[]> democraticPortionPopulation = new HashMap<String, double[]>();
        for(String key : populations.keySet()) {
            String country = key;
            Double population = populations.get(key);
            Double democraticIndexCountries = democraticIndex.get(key);
            double[] arr = new double[dates.length];
            for(int i = 0; i < arr.length; i++) {
                arr[i] = population*((democraticIndexCountries)/10);
            }
            democraticPortionPopulation.put(country, arr);
        }
        return democraticPortionPopulation;
    }

    // Get autocratic portion of Covid deaths for each country
    public static HashMap<String, double[]> autocraticPortionCovidDeaths(HashMap<String, Double> democraticIndex, String[][] covidDeaths, LocalDate[] dates) {
        HashMap<String, double[]> autocraticPortionCovidDeaths = new HashMap<String, double[]>();
        for(int i = 0; i < covidDeaths.length; i++) {
            LocalDate date = LocalDate.parse(covidDeaths[i][0], DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US));
            String country = covidDeaths[i][1];
            int deaths = Integer.parseInt(covidDeaths[i][2]);
            Double democraticIndexCountry = democraticIndex.get(country);
            int days = (int)ChronoUnit.DAYS.between(dates[0], date);
            if(autocraticPortionCovidDeaths.containsKey(country)) {
                autocraticPortionCovidDeaths.get(country)[days] = deaths*(democraticIndexCountry/10);
            } else {
                double[] arr = new double[dates.length];
                arr[days] = deaths*(democraticIndexCountry/10);
                autocraticPortionCovidDeaths.put(country, arr);
            }
        }
        return autocraticPortionCovidDeaths;
    }

    // Get autocratic portion of population for each country
    public static HashMap<String, double[]> autocraticPortionPopulation(HashMap<String, Double> democraticIndex, HashMap<String, Double> populations, LocalDate[] dates) {
        HashMap<String, double[]> autocraticPortionPopulation = new HashMap<String, double[]>();
        for(String key : populations.keySet()) {
            String country = key;
            Double population = populations.get(key);
            Double democraticIndexCountries = democraticIndex.get(key);
            double[] arr = new double[dates.length];
            for(int i = 0; i < arr.length; i++) {
                arr[i] = population*((10-democraticIndexCountries)/10);
            }
            autocraticPortionPopulation.put(country, arr);
        }
        return autocraticPortionPopulation;
    }

    // get total democratic/autocratic parts
    public static double[] addParts(HashMap<String, double[]> parts, LocalDate[] dates) {
        Collection<double[]> partsArrays = parts.values();
        double[]  timeSeries = new double[dates.length];
        for(double[] arr: partsArrays) {
            for(int i=0; i<timeSeries.length; i++) {
                timeSeries[i] += arr[i];
            }
        }
        return timeSeries;
    }

    // divide deaths by population
    public static double[] divideByPop(double[] covidDeaths, double[] pop) {
        double[] divided = new double[pop.length];
        for(int i = 0; i < covidDeaths.length; i++) {
            divided[i] = covidDeaths[i]/pop[i];
        }
        return divided;
    }

    public static void writeFile(double[] democraticDR, double[] autocraticDR, LocalDate[] dates) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("PoliticalDeathRates.csv"));
            StringBuilder sb = new StringBuilder();
            sb.append("Date,");
            for(int i = 0; i < dates.length; i++) {
                sb.append(dates[i].format(DateTimeFormatter.ofPattern("M/d/yyyy")) + ",");
            }
            writer.write(sb.toString() + System.lineSeparator());
            sb = new StringBuilder();
            sb.append("democraticDR,");
            for(int i = 0; i < democraticDR.length; i++) {
                sb.append(democraticDR[i] + ",");
            }
            writer.write(sb.toString() + System.lineSeparator());
            sb = new StringBuilder();
            sb.append("autocraticDR,");
            for(int i = 0; i < autocraticDR.length; i++) {
                sb.append(autocraticDR[i] + ",");
            }
            writer.write(sb.toString() + System.lineSeparator());
            writer.close();
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}