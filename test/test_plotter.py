

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
    content = f.read().rstrip() # remove trailling white spaces
    
    return content.replace(",", ".") # some systems use ',' for floating point in Java. However, Python expects '.', e.g. 3.1415
    
def get_full_test_name(router, test_name):
    return "{}-{}.txt".format(router, test_name)
    
def read_test_file(router, test):
    name = get_full_test_name(router, test)
    return read_file(name)

def get_test_files():
    files = get_file_names()
    extension = ".txt"
    return [file for file in files if file.endswith(extension)]
    
def plot_combined_tests():  # @incomplete
    test_files = get_test_files()
    print(test_files)

    for file in test_files:
        content = read_file(file)

        print(file)
        print(content)

def extract_test(test_content):
    lines = test_content.split("\n")
    raw_content = [line.split(" ") for line in lines]
    return [(int(seed), bool(completed), int(execution_time), float(running_time)) for (seed, completed, execution_time, running_time) in raw_content]

def get_test(router, test):
    content = read_test_file(router, test)
    return extract_test(content)

def extract_attribute_from_test(attribute_index, test_content):
    return [test[attribute_index] for test in test_content]

def plot_single_test_distribution(test_name):
    greedy_router_test = get_test(Router.GREEDY_ROUTER, test_name)
    droplet_size_router_test = get_test(Router.DROPLET_SIZE_ROUTER, test_name)
    
    import numpy as np
    import matplotlib.pyplot as plt

    ax = plt.subplot(111)
    
    greedy_seeds = extract_attribute_from_test(0, greedy_router_test)
    droplet_size_seeds = extract_attribute_from_test(0, droplet_size_router_test)
    assert(greedy_seeds == droplet_size_seeds)

    greedy_execution_times = extract_attribute_from_test(2, greedy_router_test)
    droplet_size_execution_times = extract_attribute_from_test(2, droplet_size_router_test)
    
    colors = ['C0', 'C1']
    
    #w = 0.4
    #ax.bar(np.array(greedy_seeds) - w / 2, greedy_execution_times, width=w, align='center')
    #ax.bar(np.array(droplet_size_seeds) + w / 2, droplet_size_execution_times, width=w, align='center')

    for x, ha, hb in zip(greedy_seeds, greedy_execution_times, droplet_size_execution_times):
        for i, (h, c) in enumerate(sorted(zip([ha, hb], colors))):
            ax.bar(x, h, color=c, zorder=-i)


    plt.xlabel("Seed")
    plt.ylabel("Execution Time (steps)")  
    plt.title("{} distribution".format(test_name))
    
    plt.show()    


plot_single_test_distribution("Test1")