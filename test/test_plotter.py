'''

Plots the test execution times for the routing algorithms.

'''

import numpy as np    
import matplotlib.pyplot as plt

save_figs = True # save figures or show figures


#
# Defintions
#

class Router:
    GREEDY_ROUTER = "GreedyRouter"
    DROPLET_SIZE_ROUTER = "DropletSizeAwareGreedyRouter"
    
class Attributes:
    SEED = 0
    COMPLETED = 1
    EXECUTION_TIME = 2
    RUNNING_TIME = 3


#
# Test reading
#

def get_file_names():
    import os
    return os.listdir()
    
def read_file(filename):
    import os   
    import re

    f = open(filename, "r")
    content = f.read().rstrip() # remove trailing white spaces
    
    return content.replace(",", ".") # some systems use ',' for floating point in Java. However, Python expects '.', e.g. 3.1415
    
def get_full_test_name(router, test_name):
    return "{}_{}.txt".format(router, test_name)
    
def read_test_file(router, test):
    name = get_full_test_name(router, test)
    return read_file(name)

def get_test_files():
    files = get_file_names()
    extension = ".txt"
    return [file for file in files if file.endswith(extension)]

#
# Test processing
#

def extract_test(test_content):
    lines = test_content.split("\n")
    raw_content = [line.split(" ") for line in lines]
    return [(int(seed), bool(completed), int(execution_time), float(running_time)) for (seed, completed, execution_time, running_time) in raw_content]

def get_test(router, test):
    return extract_test(read_test_file(router, test))

def get_test_by_fullname(name):
    return extract_test(read_file(name))

def extract_attribute_from_test(attribute_index, test_content):
    return [test[attribute_index] for test in test_content]

def get_average_execution_time(test_content):
    execution_times = extract_attribute_from_test(Attributes.EXECUTION_TIME, test_content)
    return np.mean(execution_times)


def get_all_average_execution_time(router, test_files):
    return [get_average_execution_time(get_test_by_fullname(file)) for file in test_files if file.startswith(router)]

#
# Plotting
#

def plot_combined_tests():
    test_files = get_test_files()
    
    greedy_average_execution_times = get_all_average_execution_time(Router.GREEDY_ROUTER, test_files)
    droplet_average_execution_times = get_all_average_execution_time(Router.DROPLET_SIZE_ROUTER, test_files)
    
    ax = plt.subplot(111)
    
    assert(len(greedy_average_execution_times) == len(droplet_average_execution_times))
    tests = np.arange(len(greedy_average_execution_times))
    
    colors = ['C0', 'C1']
    
    for x, ha, hb in zip(tests, greedy_average_execution_times, droplet_average_execution_times):
        for i, (h, c) in enumerate(sorted(zip([ha, hb], colors))):
            ax.bar(x, h, color=c, zorder=-i)
    
    plt.legend(["Greedy Router", "Droplet Size-aware Greedy Router"], loc='lower right')
    plt.xlabel("Test")
    plt.ylabel("Execution Time (steps)")  
    plt.title("Average Execution times")
    
    if save_figs:
        filename = "combined_tests"
        plt.savefig(filename)
    else:
        plt.show()   
        

def plot_single_test_distribution(test_name):
    greedy_router_test = get_test(Router.GREEDY_ROUTER, test_name)
    droplet_size_router_test = get_test(Router.DROPLET_SIZE_ROUTER, test_name)
    
    ax = plt.subplot(111)
    
    greedy_seeds = extract_attribute_from_test(Attributes.SEED, greedy_router_test)
    droplet_size_seeds = extract_attribute_from_test(Attributes.SEED, droplet_size_router_test)
    assert(greedy_seeds == droplet_size_seeds)

    greedy_execution_times = extract_attribute_from_test(Attributes.EXECUTION_TIME, greedy_router_test)
    droplet_size_execution_times = extract_attribute_from_test(Attributes.EXECUTION_TIME, droplet_size_router_test)
    
    colors = ['C0', 'C1']
    
    for x, ha, hb in zip(greedy_seeds, greedy_execution_times, droplet_size_execution_times):
        for i, (h, c) in enumerate(sorted(zip([ha, hb], colors))):
            ax.bar(x, h, color=c, zorder=-i)
    
    plt.legend(["Greedy Router", "Droplet Size-aware Greedy Router"], loc='lower right')
    plt.xlabel("Seed")
    plt.ylabel("Execution Time (steps)")  
    plt.title("{} distribution".format(test_name))
    
    if save_figs:
        filename = "single_test_dist"
        plt.savefig(filename)
    else:
        plt.show()    


plot_single_test_distribution("Test6")
plot_combined_tests()