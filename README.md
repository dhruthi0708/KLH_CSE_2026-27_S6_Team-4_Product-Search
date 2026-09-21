# **Product Search and Recommendation System for Shopping**

## **Data Structures and Algorithms – 3 (25CS2103E)**

## **Team 4**

### **Team Members**

| **ID Number** | **Name** |
|---|---|
| **2520030490** | **CHERUKUMUDI SRI DHRUTHI** |
| **2520030593** | **KACHANA MAHIMA** |
| **2520030137** | **DEEPIKAA** |

### **Supervisor**

**Dr. V Sireesha**

---

## **Abstract**

The Product Search and Recommendation System for Shopping provides an efficient way for users to search and discover products. It supports keyword and descriptive searches, spelling mistake handling, auto-complete suggestions, and filters based on price, brand, category, and ratings.

The system also supports requirement-based searches such as **"gaming laptop under ₹80,000"** by identifying user requirements, searching available products, and ranking suitable results. It recommends relevant products and provides simple explanations for the recommendations, helping users compare and select suitable products.

---

## **Requirements**

- **Java**
- **JDK 17 or above**
- **Visual Studio Code**
- **Extension Pack for Java**
- **Java Swing**
- **CSV / Text Files**
- **Windows or Linux**

---

## **Project Structure**

```text
Product-Search-Recommendation/
│
├── src/
│   └── Java source files
│
├── data/
│   ├── products.csv
│   ├── users.csv
│   └── session.txt
│
├── docs/
│   └── Project documentation
│
├── results/
│   └── Program results and screenshots
│
├── reports/
│   └── Review and final reports
│
├── README.md
└── .gitignore
```

- **`src/`** - Java source code
- **`data/`** - CSV and text data files
- **`docs/`** - Project documentation
- **`results/`** - Program results and screenshots
- **`reports/`** - Review and final reports
- **`README.md`** - Project information and instructions

---

## **Setup Instructions**

### **1. Check Java Installation**

Open a terminal and run:

```bash
java -version
javac -version
```

**JDK 17 or above is required.**

### **2. Open the Project**

Open the project folder in **Visual Studio Code**.

### **3. Check Project Data**

Make sure the required files are available inside the `data` folder:

```text
data/
├── products.csv
├── users.csv
└── session.txt
```

### **4. Compile the Project**

From the project root directory, run:

**Windows:**

```bash
javac -d bin src\*.java
```

**Linux / macOS:**

```bash
javac -d bin src/*.java
```

### **5. Run the Project**

**Windows / Linux / macOS:**

```bash
java -cp bin Main
```

---

## **Features**

- Product search
- Spelling mistake handling
- Auto-complete suggestions
- Price filtering
- Brand filtering
- Category filtering
- Requirement-based search
- Similar product recommendations
- Explainable recommendations
- User authentication
- Shopping cart management
- Search and product processing

---

## **Example Search**

The system supports requirement-based searches such as:

```text
gaming laptop under ₹80,000
```

The system identifies the product requirements and searches the available products to find and rank suitable results.

---

## **Source Files**

| **File** | **Module** | **Purpose** |
|---|---|---|
| **ProblemClassifier.java** | **CO1** | Query classification and algorithm selection |
| **PatternSearch.java** | **CO2** | Naive, KMP, Z-function and Rabin-Karp |
| **SimilaritySearch.java** | **CO2 / CO3** | Similarity and sequence-based searching |
| **FuzzySearch.java** | **CO3** | Spelling correction and "Did you mean" suggestions |
| **NetworkFlow.java** | **CO4** | Network flow and matching algorithms |
| **GreedyRecommendation.java** | **CO3 / CO5** | Recommendation and optimization algorithms |
| **ParallelSearch.java** | **CO6** | Parallel and randomized algorithms |
| **SearchEngine.java** | **-** | Product scoring and recommendation explanations |
| **ProductRepository.java** | **-** | Product data loading and indexing |
| **Product.java** | **-** | Product data model |
| **Text.java** | **-** | Text processing and tokenization |
| **Auth.java** | **-** | User authentication |
| **Cart.java** | **-** | Shopping cart management |
| **IntList.java** | **-** | Integer list data structure |
| **StrList.java** | **-** | String list data structure |
| **IntQueue.java** | **-** | Integer queue data structure |
| **HashMapStr.java** | **-** | Hash map implementation |
| **Rand.java** | **-** | Random number utilities |

---

## **Current Phase Status**

**Current Phase:** Project Implementation / Review Phase

The project source code and data are being maintained through GitHub. Documentation, results, reports, and other project deliverables will be added and updated progressively.

---

## **Expected Outcome**

The system aims to provide faster and more relevant product search results, handle spelling mistakes, support requirement-based searches, rank suitable products, and recommend relevant products.

The system is intended to reduce search time and make product comparison and selection easier for users.

---

## **Version Control**

The project is maintained using **GitHub**.

Each team member contributes using their **own GitHub account** so that individual contributions can be verified through the commit history.

Meaningful commits will be made progressively throughout the project.

### **Phase Deliverables**

The major phase deliverables will be tagged as:

```text
review-1
review-2
review-3
final
```

---

## **Project Documentation**

Project documentation and supporting materials will be maintained in the following folders:

```text
docs/
results/
reports/
```

These folders will contain the relevant documentation, results, screenshots, review materials, and final project reports.
