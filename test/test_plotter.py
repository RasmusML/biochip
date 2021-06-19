
class Router:
    GREEDY_ROUTER = "GreedyRouter"
    DROPLET_SIZE_ROUTER = "DropletSizeAwareGreedyRouter"

def get_file_names():
    import os
    return os.listdir()
    
def read_file(filename):
    import os   
    import re

    f = open(filename, "r")
    content = f.read()
    
    return content

def get_test_name(router, test):
    return "{}-{}.txt".format(router, test)
    
def read_test_file(router, test):
    name = get_test_name(router, test)
    return read_file(name)

def get_test_files():
    files = get_file_names()
    extension = ".txt"
    return [file for file in files if file.endswith(extension)]
    

def plot_single_test_distribution(test_name="Test1"):
    greedy_router_test = read_test_file(Router.GREEDY_ROUTER, test_name)
    droplet_size_router_test = read_test_file(Router.DROPLET_SIZE_ROUTER, test_name)
    
    import matplotlib.pyplot as plt

    fig = plt.figure()
    ax = fig.add_axes([0,0,1,1])
    
    seeds = [1, 2]
    execution_times = [10, 14]
    ax.bar(seeds, execution_times)
    
    plt.show()

#test_name = get_test_name(Router.GREEDY_ROUTER, "Test1")
#file = read_file(test_name)

test_files = get_test_files()
print(test_files)

for file in test_files:
    content = read_file(file)

    print(file)
    print(content)

    
plot_single_test_distribution()