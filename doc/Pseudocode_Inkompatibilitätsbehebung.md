# Pseudocode

```java
public class DependencyCheckResult{
    private String name;
    private IncompatibilityType type;
    private boolean incompatible;
    private List<Dependency> conflictingDependencies; 
    private String actualVersion;
    private List<String> fixingVersions;
    
    public DependencyCheckResult(...){...}
    ...
    //Getter, Setter and so on
}

public class Dependency{
    private String name;
    private String version;
    private boolean fixed;
    private FileInputStream sourceCode;
    private FileInputStream byteCode;
    private List<Dependency> transitiveDependencies;
    
    public Dependency(...){...}
    ...
    //Getter, Setter and so on    
}

interface StrategyDeterminer{
    List<IncompatibilityChecker> checkStrategies;
    List<IncompatibilityFixer> fixStrategies;
    IncompatibilityFixer lastUsedFixer;
    IncompatibilityChecker lastUsedChecker;
    IncompatibilityChecker determineChecker(Dependency dependency);
    IncompatibilityFixer getFixingStrategyFromDatabase(DependencyCheckResult checkResult);
    IncompatibilityFixer doDetermineFixer();
    default IncompatibilityFixer determineFixer(DependencyCheckResult checkResult) throws NotDetermineableException(){
        IncompatibilityFixer fixer;
        
        fixer = getFixingStrategyFromDatabase(checkResult);
        if(fixingVersions.isEmpty()){
            fixer = doDetermineFixer(checkResult)
            ...
        }
        ...
        return fixer
    }
}

public abstract class IncompatibilityChecker {
    public DependencyCheckResult check(Dependency dependency){
        DependencyCheckResult result;
        ...
        result = getVersionInformationFromDatabase(dependency);
        if(fixingVersions.isEmpty()){
            //Check Logic
            result = doCheck(dependency);
            ...
        }
        
        ...
        return result; 
    }
    
    public abstract boolean doCheck();
    ...
}

public abstract class IncompatibilityFixer {
    public abstract boolean fix(DependencyCheckResult checkResult);
    public abstract boolean rebuildProjectWithUpdatedDependencies();
}

public class IncompatibilityResolver {
    private StrategyDeterminer determiner;
    private IncompatibilityChecker checker;
    private IncompatibilityFixer fixer;
    
    public IncompatibilityResolver(){...}
    
    public void resolve(List<Dependency> dependencies, boolean isRebuild) throws Exception{
        boolean resolvable = false;
        List<Dependency> fixedDependencies = new ArrayList<>();
        Listy<DependencyCheckResult> unresolvedDependencies = new ArrayList<>();
        
        for (Dependency dependency : dependencies) {
            DependencyCheckResult checkResult;
            boolean checkDone = false;
            
            do{
				checker = determiner.determineChecker(dependency);
				
				if(checker == null){
                    break;
                }
            	checkResult = checker.check(dependency);         
                
                if(checkResult.isIncompatible()){
                    checkDone = true;
                    boolean fixDone = false;
                    
                    do{
                        try{
                            fixer = determiner.determineFixer(checkResult);
                            if(fixer.fix(checkResult)){
                                fixDone = true;
                                dependency.setFixed(true);
                                fixedDependencies.add(dependency);
                            }
                        } catch (NotDetermineableException e){
                            unresolvedDependencies.add(checkResult);
                            fixDone = true;
                        }
					...
                    } while(!fixDone);
                }
            	...
            } while(!checkDone); 
        }
        
        if(!fixedDependencies.isEmpty && unresolvedDependencies.isEmpty() && !isRebuild) {
            fixer.rebuildProjectWithUpdatedDependencies();
            resolve(fixedDependencies, true);
        }
        
        if(unresolvedDependencies.isEmpty()){
            generateFixReport(fixedDependencies);
        } else{
            generateBugreport(unresolvedDependencies, isRebuild);
            throw new SomeRuntimeException(unresolvedDependencies);
        }
    }
    ...
}
```

